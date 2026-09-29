/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.upload.domain.manager

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.drive.base.domain.log.LogTag.UploadTag.logTag
import me.proton.core.drive.base.domain.provider.ProtonDriveClientProvider
import me.proton.core.drive.base.domain.provider.ProtonPhotosClientProvider
import me.proton.core.drive.link.domain.entity.NodeContext
import me.proton.core.drive.upload.domain.exception.UploadNotFoundException
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.ProtonDriveClient
import me.proton.drive.sdk.ProtonPhotosClient
import me.proton.drive.sdk.UploadController
import me.proton.drive.sdk.Uploader
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadSdkManager @Inject constructor(
    private val protonDriveClientProvider: ProtonDriveClientProvider,
    private val protonPhotosClientProvider: ProtonPhotosClientProvider,
) {

    private data class UploadState(
        val mutex: Mutex,
        var uploader: Uploader? = null,
        var controller: UploadController? = null
    )

    private val states = ConcurrentHashMap<Long, UploadState>()

    suspend fun enqueue(
        nodeContext: NodeContext,
        uploadFileLinkId: Long,
        block: suspend (ProtonDriveClient) -> Uploader,
    ) {
        with(uploadFileLinkId.state()) {
            mutex.withLock {
                if (uploader == null) {
                    CoreLogger.d(uploadFileLinkId.logTag(), "Creating file uploader")
                    val driveClient = protonDriveClientProvider
                        .getOrCreate(nodeContext.userId)
                        .getOrThrow()
                    uploader = block(driveClient)
                }
            }
        }
    }

    suspend fun enqueuePhoto(
        nodeContext: NodeContext,
        uploadFileLinkId: Long,
        block: suspend (ProtonPhotosClient) -> Uploader,
    ) {
        with(uploadFileLinkId.state()) {
            mutex.withLock {
                if (uploader == null) {
                    CoreLogger.i(
                        tag = uploadFileLinkId.logTag(),
                        message = "Creating photos uploader",
                    )
                    val photosClient = protonPhotosClientProvider
                        .getOrCreate(nodeContext.userId)
                        .getOrThrow()
                    uploader = block(photosClient)
                }
            }
        }
    }

    suspend fun controller(
        uploadFileLinkId: Long,
        block: suspend (Uploader) -> UploadController
    ): UploadController = with(uploadFileLinkId.state()) {
        mutex.withLock {
            val uploader = uploader
                ?: throw UploadNotFoundException("Upload was not enqueued or cancelled")

            suspend fun createController(): UploadController {
                CoreLogger.i(
                    tag = uploadFileLinkId.logTag(),
                    message = "Creating controller",
                )
                return block(uploader)
            }
            controller ?: createController().also { controller = it }
        }
    }

    suspend fun close(uploadFileLinkId: Long) {
        val id = uploadFileLinkId
        val state = states.remove(id) ?: return
        with(state) {
            CoreLogger.d(
                id.logTag(), "Closing sdk: " +
                        "uploader: ${uploader != null}, " +
                        "controller: ${controller != null}"
            )

            mutex.withLock {
                controller?.close()
                uploader?.close()
            }
        }
    }

    suspend fun cancel(uploadFileLinkId: Long) {
        val id = uploadFileLinkId
        val state = states.remove(id) ?: return
        with(state) {
            CoreLogger.d(
                id.logTag(), "Cancelling sdk: " +
                        "uploader: ${uploader != null}, " +
                        "controller: ${controller != null}"
            )
            mutex.withLock {
                controller?.apply {
                    cancel()
                    dispose()
                    close()
                }
                uploader?.apply {
                    cancel()
                    close()
                }
            }
        }
    }

    private fun Long.state(): UploadState =
        states.computeIfAbsent(this) {
            UploadState(mutex = Mutex())
        }
}
