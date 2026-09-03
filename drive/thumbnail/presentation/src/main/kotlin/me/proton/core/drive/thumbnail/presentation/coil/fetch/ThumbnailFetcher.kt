/*
 * Copyright (c) 2022-2023 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.thumbnail.presentation.coil.fetch

import android.content.Context
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag.THUMBNAIL
import me.proton.core.drive.crypto.domain.usecase.DecryptThumbnail
import me.proton.core.drive.drivelink.domain.usecase.UseSdkForThumbnail
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailDecryptedFile
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailFile
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailInputStream
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailSdk
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.util.kotlin.CoreLogger
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

object ThumbnailKeyer : Keyer<ThumbnailVO> {
    override fun key(data: ThumbnailVO, options: Options): String = with(data) {
        "$volumeId-$revisionId-${thumbnailId.type}"
    }
}

@OptIn(ExperimentalCoilApi::class)
class ThumbnailFetcher(
    private val context: Context,
    private val getThumbnailInputStream: GetThumbnailInputStream,
    private val getThumbnailFile: GetThumbnailFile,
    private val getThumbnailDecryptedFile: GetThumbnailDecryptedFile,
    private val getThumbnailSdk: GetThumbnailSdk,
    private val decryptThumbnail: DecryptThumbnail,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
    private val useSdkForThumbnail: UseSdkForThumbnail,
    private val data: ThumbnailVO,
    private val options: Options
) : Fetcher {

    class ThumbnailMetadata(val fileId: FileId) : ImageSource.Metadata()

    private fun getSource(data: ThumbnailVO, bufferedSource: BufferedSource) = ImageSource(
        source = bufferedSource,
        context = context,
        metadata = ThumbnailMetadata(data.fileId)
    )

    override suspend fun fetch(): FetchResult {
        requireNotNull(data.revisionId) { "A file without a revision doesn't have a thumbnail" }
        val encryptedThumbnailFile = getThumbnailFile(data.fileId.userId, data.volumeId, data.revisionId, data.thumbnailId.type)
        val decryptedThumbnailFile = getThumbnailDecryptedFile(
            userId = data.fileId.userId,
            volumeId = data.volumeId,
            revisionId = data.revisionId,
            type = data.thumbnailId.type,
            inCacheFolder = !isLinkOrAnyAncestorMarkedAsOffline(data.fileId)
        )
        val allowNetwork = options.networkCachePolicy.readEnabled
        val allowDiskRead = options.diskCachePolicy.readEnabled
        val allowDiskWrite = options.diskCachePolicy.writeEnabled
        return when {
            allowDiskRead && decryptedThumbnailFile.existsAndNotEmpty() -> {
                SourceResult(
                    getSource(data, decryptedThumbnailFile.source().buffer()),
                    mimeType = null,
                    dataSource = DataSource.DISK,
                )
            }

            allowDiskRead && allowDiskWrite && encryptedThumbnailFile.existsAndNotEmpty() -> {
                if (encryptedThumbnailFile != null && encryptedThumbnailFile.existsAndNotEmpty()) {
                    if (!decryptedThumbnailFile.exists()) {
                        decryptedThumbnailFile.createNewFile()
                    }
                    decryptedThumbnailFile.outputStream().use { outputStream ->
                        outputStream.write(
                            decryptThumbnail(
                                data.fileId,
                                encryptedThumbnailFile.inputStream()
                            ).getOrThrow()
                        )
                    }
                    encryptedThumbnailFile.delete()
                }
                SourceResult(
                    getSource(data, decryptedThumbnailFile.source().buffer()),
                    mimeType = null,
                    dataSource = DataSource.DISK,
                )
            }

            allowNetwork -> fetchFromNetwork(
                data = data,
                options = options,
                cacheFile = decryptedThumbnailFile
            )

            else -> throw IllegalArgumentException("Couldn't access the thumbnail")
        }
    }

    private fun File?.existsAndNotEmpty() = this != null && exists() && length() > 0

    private suspend fun fetchFromNetwork(
        data: ThumbnailVO,
        options: Options,
        cacheFile: File,
    ): SourceResult = if (useSdkForThumbnail(data.fileId).getOrThrow()) {
        getThumbnailSdk(
            volumeId = data.volumeId,
            fileId = data.fileId,
            thumbnailType = data.thumbnailId.type,
        ).map { inputStream ->
            inputStream.use {
                writeOnDiskIfNeeded(
                    options = options,
                    cacheFile = cacheFile,
                    data = data,
                    inputStream = inputStream,
                )
            }
        }.onFailure { error ->
            error.log(
                THUMBNAIL,
                "Error while fetching thumbnail with sdk ${data.thumbnailId.id}"
            )
        }.getOrThrow()
    } else {
        getThumbnailInputStream(
            thumbnailId = data.thumbnailId,
        ).map { inputStream ->
            inputStream.use {
                writeOnDiskIfNeeded(
                    options = options,
                    cacheFile = cacheFile,
                    data = data,
                    inputStream = ByteArrayInputStream(decryptThumbnail(data.fileId, inputStream).getOrThrow())
                )
            }
        }.onFailure { error ->
            error.log(
                THUMBNAIL,
                "Error while fetching thumbnail with legacy ${data.thumbnailId.id}"
            )
        }.getOrThrow()
    }

    private fun writeOnDiskIfNeeded(
        options: Options,
        cacheFile: File,
        data: ThumbnailVO,
        inputStream: InputStream,
    ): SourceResult {
        val allowDiskWrite = options.diskCachePolicy.writeEnabled
        return if (allowDiskWrite) {
            if (!cacheFile.exists()) {
                cacheFile.createNewFile()
            }
            cacheFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            CoreLogger.d(THUMBNAIL, "Thumbnail cache file size: ${cacheFile.length()} bytes for ${data.thumbnailId.id}")
            cacheFile.inputStream()
        } else {
            inputStream
        }.let { inputStream ->
            SourceResult(
                source = getSource(data, inputStream.source().buffer()),
                mimeType = null,
                dataSource = DataSource.NETWORK,
            )
        }
    }

    class Factory constructor(
        private val context: Context,
        private val getThumbnailInputStream: GetThumbnailInputStream,
        private val getThumbnailFile: GetThumbnailFile,
        private val getThumbnailDecryptedFile: GetThumbnailDecryptedFile,
        private val getThumbnailSdk: GetThumbnailSdk,
        private val useSdkForThumbnail: UseSdkForThumbnail,
        private val decryptThumbnail: DecryptThumbnail,
        private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
    ) : Fetcher.Factory<ThumbnailVO> {
        override fun create(
            data: ThumbnailVO,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            return ThumbnailFetcher(
                context = context,
                getThumbnailInputStream = getThumbnailInputStream,
                getThumbnailFile = getThumbnailFile,
                getThumbnailDecryptedFile = getThumbnailDecryptedFile,
                getThumbnailSdk = getThumbnailSdk,
                useSdkForThumbnail = useSdkForThumbnail,
                decryptThumbnail = decryptThumbnail,
                isLinkOrAnyAncestorMarkedAsOffline = isLinkOrAnyAncestorMarkedAsOffline,
                data = data,
                options = options,
            )
        }
    }

    companion object {
        const val MIME_TYPE = "image/proton-encrypted"
    }
}
