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

import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.size.Size
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailCacheFile
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailInputStream
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailFetcher @Inject constructor(
    private val getThumbnailInputStream: GetThumbnailInputStream,
    private val getThumbnailCacheFile: GetThumbnailCacheFile,
) : Fetcher<ThumbnailVO> {

    override suspend fun fetch(pool: BitmapPool, data: ThumbnailVO, size: Size, options: Options): FetchResult {
        val revisionId = requireNotNull(data.revisionId) { "A file without a revision doesn't have a thumbnail" }
        val cacheFile = getThumbnailCacheFile(data.fileId.userId, data.volumeId, data.revisionId)
        val allowNetwork = options.networkCachePolicy.readEnabled
        val allowDiskRead = options.diskCachePolicy.readEnabled
        return when {
            allowDiskRead && cacheFile.exists() && cacheFile.length() > 0 -> SourceResult(
                Source(data, cacheFile.inputStream()),
                mimeType = MIME_TYPE,
                dataSource = DataSource.DISK,
            )
            allowNetwork -> fetchFromNetwork(data, revisionId, options, cacheFile)
            else -> throw IllegalArgumentException("Couldn't access the thumbnail")
        }
    }

    private suspend fun fetchFromNetwork(
        data: ThumbnailVO,
        revisionId: String,
        options: Options,
        cacheFile: File,
    ): SourceResult = getThumbnailInputStream(
        fileId = data.fileId,
        revisionId = revisionId,
    ).map { inputStream ->
        inputStream.use {
            writeOnDiskIfNeeded(options, cacheFile, inputStream, data)
        }
    }.getOrThrow()

    private fun writeOnDiskIfNeeded(
        options: Options,
        cacheFile: File,
        networkInputStream: InputStream,
        data: ThumbnailVO,
    ): SourceResult {
        val allowDiskWrite = options.diskCachePolicy.writeEnabled
        return if (allowDiskWrite) {
            if (!cacheFile.exists()) {
                cacheFile.createNewFile()
            }
            cacheFile.outputStream().use { outputStream ->
                networkInputStream.copyTo(outputStream)
            }
            cacheFile.inputStream()
        } else {
            networkInputStream
        }.let { inputStream ->
            SourceResult(
                source = Source(data, inputStream),
                mimeType = MIME_TYPE,
                dataSource = DataSource.NETWORK,
            )
        }
    }

    override fun key(data: ThumbnailVO): String = with(data) {
        "$volumeId-$revisionId"
    }

    class Source(
        val thumbnailVO: ThumbnailVO,
        inputStream: InputStream,
    ) : BufferedSource by inputStream.source().buffer()

    companion object {
        const val MIME_TYPE = "image/proton-encrypted"
    }
}
