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
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailDecryptedFile
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailSdk
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.thumbnail.presentation.extension.cacheKey
import me.proton.core.drive.thumbnail.presentation.extension.revisionContext
import me.proton.core.util.kotlin.CoreLogger
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File
import java.io.InputStream

object ThumbnailKeyer : Keyer<ThumbnailVO> {
    override fun key(data: ThumbnailVO, options: Options): String = data.cacheKey
}

@OptIn(ExperimentalCoilApi::class)
class ThumbnailFetcher(
    private val context: Context,
    private val getThumbnailDecryptedFile: GetThumbnailDecryptedFile,
    private val getThumbnailSdk: GetThumbnailSdk,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
    private val data: ThumbnailVO,
    private val options: Options
) : Fetcher {

    private fun getSource(bufferedSource: BufferedSource) = ImageSource(
        source = bufferedSource,
        context = context,
    )

    override suspend fun fetch(): FetchResult {
        val thumbnailFile = getThumbnailDecryptedFile(
            userId = data.userId,
            revisionUid = data.revisionUid,
            type = data.type,
            inCacheFolder = !isLinkOrAnyAncestorMarkedAsOffline(data.userId, data.revisionUid.nodeUid),
        )
        val allowNetwork = options.networkCachePolicy.readEnabled
        val allowDiskRead = options.diskCachePolicy.readEnabled
        return when {
            allowDiskRead && thumbnailFile.existsAndNotEmpty() -> {
                SourceResult(
                    getSource(thumbnailFile.source().buffer()),
                    mimeType = null,
                    dataSource = DataSource.DISK,
                )
            }

            allowNetwork -> fetchFromNetwork(
                data = data,
                options = options,
                cacheFile = thumbnailFile,
            )

            else -> throw IllegalArgumentException("Couldn't access the thumbnail")
        }
    }

    private fun File?.existsAndNotEmpty() = this != null && exists() && length() > 0

    private suspend fun fetchFromNetwork(
        data: ThumbnailVO,
        options: Options,
        cacheFile: File,
    ): SourceResult = getThumbnailSdk(
        revisionContext = data.revisionContext,
        thumbnailType = data.type,
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
            "Error while fetching thumbnail for ${data.revisionUid.value}"
        )
    }.getOrThrow()

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
            CoreLogger.d(
                THUMBNAIL,
                "Thumbnail cache file size: ${cacheFile.length()} bytes for ${data.revisionUid.value}"
            )
            cacheFile.inputStream()
        } else {
            inputStream
        }.let { inputStream ->
            SourceResult(
                source = getSource(inputStream.source().buffer()),
                mimeType = null,
                dataSource = DataSource.NETWORK,
            )
        }
    }

    class Factory constructor(
        private val context: Context,
        private val getThumbnailDecryptedFile: GetThumbnailDecryptedFile,
        private val getThumbnailSdk: GetThumbnailSdk,
        private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
    ) : Fetcher.Factory<ThumbnailVO> {
        override fun create(
            data: ThumbnailVO,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return ThumbnailFetcher(
                context = context,
                getThumbnailDecryptedFile = getThumbnailDecryptedFile,
                getThumbnailSdk = getThumbnailSdk,
                isLinkOrAnyAncestorMarkedAsOffline = isLinkOrAnyAncestorMarkedAsOffline,
                data = data,
                options = options,
            )
        }
    }
}
