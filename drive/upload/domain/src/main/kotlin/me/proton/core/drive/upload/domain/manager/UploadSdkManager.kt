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
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
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

    suspend fun enqueue(uploadFileLink: UploadFileLink, block: suspend (ProtonDriveClient) -> Uploader) {
        with(uploadFileLink.state()) {
            mutex.withLock {
                if (uploader == null) {
                    CoreLogger.d(uploadFileLink.id.logTag(), "Creating file uploader")
                    val driveClient = protonDriveClientProvider
                        .getOrCreate(uploadFileLink.userId)
                        .getOrThrow()
                    uploader = block(driveClient)
                }
            }
        }
    }

    suspend fun enqueuePhoto(
        uploadFileLink: UploadFileLink,
        block: suspend (ProtonPhotosClient) -> Uploader
    ) {
        with(uploadFileLink.state()) {
            mutex.withLock {
                if (uploader == null) {
                    CoreLogger.i(
                        tag = uploadFileLink.id.logTag(),
                        message = "Creating photos uploader",
                    )
                    val photosClient = protonPhotosClientProvider
                        .getOrCreate(uploadFileLink.userId)
                        .getOrThrow()
                    uploader = block(photosClient)
                }
            }
        }
    }

    suspend fun controller(
        uploadFileLink: UploadFileLink,
        block: suspend (Uploader) -> UploadController
    ): UploadController = with(uploadFileLink.state()) {
        mutex.withLock {
            val uploader = uploader
                ?: throw UploadNotFoundException("Upload was not enqueued or cancelled")

            suspend fun createController(): UploadController {
                CoreLogger.i(
                    tag = uploadFileLink.id.logTag(),
                    message = "Creating controller",
                )
                return block(uploader)
            }
            controller ?: createController().also { controller = it }
        }
    }

    suspend fun close(uploadFileLink: UploadFileLink) {
        val id = uploadFileLink.id
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

    suspend fun cancel(uploadFileLink: UploadFileLink) {
        val id = uploadFileLink.id
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

    suspend fun cancelController(uploadFileLink: UploadFileLink) {
        val id = uploadFileLink.id
        val state = states[id] ?: return
        with(state) {
            CoreLogger.d(
                id.logTag(), "Cancelling sdk controller: ${uploader != null}"
            )
            mutex.withLock {
                controller?.apply {
                    cancel()
                    dispose()
                    close()
                }
                controller = null
            }
        }
    }

    private fun UploadFileLink.state(): UploadState =
        states.computeIfAbsent(id) {
            UploadState(mutex = Mutex())
        }
}
