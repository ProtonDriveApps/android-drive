/*
 * Copyright (c) 2026 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.ui.screen.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.Options
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.provider.ProtonSdkClientProvider
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailDecryptedFile
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.drive.sdk.entity.RevisionUid
import okio.Buffer
import okio.buffer
import okio.source
import me.proton.drive.sdk.entity.ThumbnailType as SdkThumbnailType

@OptIn(ExperimentalCoilApi::class)
@Singleton
internal class PhotoMapThumbnails @Inject constructor(
    private val getThumbnailDecryptedFile: GetThumbnailDecryptedFile,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
    private val protonSdkClientProvider: ProtonSdkClientProvider,
    private val limiter: PhotoMapDownloadLimiter,
) : Fetcher.Factory<ThumbnailVO> {
    fun openSession(scope: CoroutineScope): Session =
        Session(CoroutineScope(scope.coroutineContext + Dispatchers.IO), limiter, protonSdkClientProvider)

    @Composable
    fun painter(thumbnail: ThumbnailVO, download: Boolean, session: Session): AsyncImagePainter =
        key(download, session) {
            val context = LocalContext.current
            val request = remember(context, thumbnail) {
                ImageRequest.Builder(context)
                    .data(thumbnail)
                    .fetcherFactory(this, ThumbnailVO::class.java)
                    .networkCachePolicy(if (download) CachePolicy.ENABLED else CachePolicy.DISABLED)
                    .setParameter(SESSION, session, memoryCacheKey = null)
                    .build()
            }
            rememberAsyncImagePainter(request, contentScale = ContentScale.Crop)
        }

    override fun create(data: ThumbnailVO, options: Options, imageLoader: ImageLoader): Fetcher =
        object : Fetcher {
            override suspend fun fetch(): SourceResult = load(data, options)
        }

    private suspend fun load(thumbnail: ThumbnailVO, options: Options): SourceResult {
        val nodeUid = thumbnail.revisionUid.nodeUid
        val cacheFile = getThumbnailDecryptedFile(
            userId = thumbnail.userId,
            revisionUid = thumbnail.revisionUid,
            type = thumbnail.type,
            inCacheFolder = !isLinkOrAnyAncestorMarkedAsOffline(thumbnail.userId, nodeUid),
        )
        fun cached(): SourceResult? = if (options.diskCachePolicy.readEnabled && cacheFile.hasThumbnail()) {
            SourceResult(ImageSource(cacheFile.source().buffer(), options.context), null, DataSource.DISK)
        } else {
            null
        }
        cached()?.let { return it }
        if (!options.networkCachePolicy.readEnabled) throw IOException("Thumbnail is not cached")

        val session = requireNotNull(options.parameters.value<Session>(SESSION))
        val bytes = session.load(
            thumbnail = thumbnail,
            cacheFile = cacheFile,
            readCache = options.diskCachePolicy.readEnabled,
            writeCache = options.diskCachePolicy.writeEnabled,
        )
        return if (bytes == null) {
            cached() ?: throw IOException("Cached thumbnail is unavailable")
        } else {
            SourceResult(ImageSource(Buffer().write(bytes), options.context), null, DataSource.NETWORK)
        }
    }

    internal class Session(
        scope: CoroutineScope,
        limiter: PhotoMapDownloadLimiter,
        provider: ProtonSdkClientProvider,
    ) {
        private data class BatchKey(
            val userId: UserId,
            val volumeType: Volume.Type,
            val type: SdkThumbnailType,
        )

        private data class Target(
            val revisionUid: RevisionUid,
            val cacheFile: File,
            val readCache: Boolean,
            val writeCache: Boolean,
        )

        private sealed interface Data {
            data object Cached : Data
            data class Downloaded(val bytes: ByteArray) : Data
        }

        private val queue = ThumbnailDownloadThrottle<BatchKey, Target, Data>(
            parentScope = scope,
            cached = { target -> Data.Cached.takeIf { target.readCache && target.cacheFile.hasThumbnail() } },
            runTransfer = { transfer -> limiter.download(PhotoMapDownloadKind.THUMBNAIL, transfer) },
            prepareDownload = { key ->
                val client = provider.getOrCreate(key.userId, key.volumeType).getOrThrow()
                val download: (List<Target>) -> Flow<Pair<Target, Result<Data>>> = { targets ->
                    flow {
                        val byNodeUid = targets.groupBy { it.revisionUid.nodeUid }
                        client.enumerateThumbnails(byNodeUid.keys.toList(), key.type).collect { thumbnail ->
                            for (target in byNodeUid[thumbnail.uid].orEmpty()) {
                                val result = thumbnail.result.mapCatching { bytes ->
                                    if (bytes.isEmpty()) throw IOException("Thumbnail is empty")
                                    if (target.writeCache) target.cacheFile.saveThumbnail(bytes)
                                    Data.Downloaded(bytes)
                                }
                                emit(target to result)
                            }
                        }
                    }
                }
                download
            },
        )
        val progress = queue.progress

        fun cancel() = queue.cancel()

        suspend fun load(
            thumbnail: ThumbnailVO,
            cacheFile: File,
            readCache: Boolean,
            writeCache: Boolean,
        ): ByteArray? {
            val type = when (thumbnail.type) {
                ThumbnailType.DEFAULT -> SdkThumbnailType.THUMBNAIL
                ThumbnailType.PHOTO -> SdkThumbnailType.PREVIEW
            }
            val key = BatchKey(thumbnail.userId, thumbnail.volumeType, type)
            val target = Target(thumbnail.revisionUid, cacheFile, readCache, writeCache)
            return when (val data = queue.load(key, target)) {
                Data.Cached -> null
                is Data.Downloaded -> data.bytes
            }
        }
    }

    private companion object {
        const val SESSION = "photoMapThumbnailDownloadSession"
    }
}

private fun File.hasThumbnail() = isFile && length() > 0

private fun File.saveThumbnail(bytes: ByteArray) {
    val directory = requireNotNull(parentFile)
    try {
        val temporary = Files.createTempFile(directory.toPath(), "map-thumbnail-", ".tmp")
        try {
            Files.write(temporary, bytes)
            Files.move(temporary, toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    } catch (_: IOException) {
        // The downloaded thumbnail is still usable when the cache cannot be written.
    }
}
