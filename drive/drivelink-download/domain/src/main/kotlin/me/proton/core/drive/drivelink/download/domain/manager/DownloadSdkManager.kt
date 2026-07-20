/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.drivelink.download.domain.manager

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.DOWNLOAD
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.provider.ProtonPhotosClientProvider
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.revisionUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.DownloadController
import me.proton.drive.sdk.Downloader
import me.proton.drive.sdk.ProtonDriveClient
import me.proton.drive.sdk.ProtonPhotosClient
import me.proton.drive.sdk.entity.RevisionUid
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadSdkManager @Inject constructor(
    private val protonDriveClientProvider: ProtonDriveClientProvider,
    private val protonPhotosClientProvider: ProtonPhotosClientProvider,
) {

    private data class DownloadState(
        val mutex: Mutex,
        var downloader: Downloader? = null,
        var controller: DownloadController? = null
    )

    private val states = ConcurrentHashMap<RevisionUid, DownloadState>()

    suspend fun enqueueFile(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
        block: suspend (ProtonDriveClient) -> Downloader
    ) {
        val nodeRevisionUid = fileId.revisionUid(
            volumeId = volumeId,
            revisionId = revisionId,
        )
        with(nodeRevisionUid.state()) {
            mutex.withLock {
                if (downloader == null) {
                    CoreLogger.d(fileId.logTag, "Creating drive downloader")
                    val driveClient = protonDriveClientProvider
                        .getOrCreate(fileId.userId)
                        .getOrThrow()
                    downloader = block(driveClient)
                }
            }
        }
    }

    suspend fun enqueuePhoto(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
        block: suspend (ProtonPhotosClient) -> Downloader
    ) {
        val nodeRevisionUid = fileId.revisionUid(
            volumeId = volumeId,
            revisionId = revisionId,
        )
        with(nodeRevisionUid.state()) {
            mutex.withLock {
                if (downloader == null) {
                    CoreLogger.d(fileId.logTag, "Creating photo downloader")
                    val photosClient = protonPhotosClientProvider
                        .getOrCreate(fileId.userId)
                        .getOrThrow()
                    downloader = block(photosClient)
                }
            }
        }
    }

    suspend fun controller(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
        block: suspend (Downloader) -> DownloadController
    ): DownloadController {
        val nodeRevisionUid = fileId.revisionUid(
            volumeId = volumeId,
            revisionId = revisionId,
        )
        return with(nodeRevisionUid.state()) {
            mutex.withLock {
                val downloader = this.downloader
                    ?: error("Download was not enqueued or cancelled")

                controller ?: block(downloader).also { controller = it }
            }
        }
    }

    suspend fun close(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
    ) {
        val nodeRevisionUid = fileId.revisionUid(
            volumeId = volumeId,
            revisionId = revisionId,
        )
        val state = states.remove(nodeRevisionUid) ?: return
        with(state) {
            CoreLogger.d(
                DOWNLOAD, "Closing sdk: " +
                        "downloader: ${downloader != null}, " +
                        "controller: ${controller != null}"
            )

            mutex.withLock {
                controller?.close()
                downloader?.close()
            }
        }
    }

    suspend fun cancel(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
    ) {
        val nodeRevisionUid = fileId.revisionUid(
            volumeId = volumeId,
            revisionId = revisionId,
        )
        val state = states.remove(nodeRevisionUid) ?: return
        with(state) {
            CoreLogger.d(
                DOWNLOAD, "Cancelling sdk: " +
                        "downloader: ${downloader != null}, " +
                        "controller: ${controller != null}"
            )
            mutex.withLock {
                controller?.apply {
                    cancel()
                    close()
                }
                downloader?.apply {
                    cancel()
                    close()
                }
            }
        }
    }

    suspend fun cancelController(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String
    ) {
        val nodeRevisionUid = fileId.revisionUid(
            volumeId = volumeId,
            revisionId = revisionId,
        )
        val state = states.remove(nodeRevisionUid) ?: return
        with(state) {
            CoreLogger.d(
                DOWNLOAD, "Cancelling sdk controller: ${downloader != null}"
            )
            mutex.withLock {
                controller?.apply {
                    cancel()
                    close()
                }
                controller = null
            }
        }
    }

    private fun RevisionUid.state(): DownloadState =
        states.computeIfAbsent(this) {
            DownloadState(mutex = Mutex())
        }

    private val FileId.logTag: String get() = "${LogTag.DOWNLOAD}.${id.logId()}"
}
