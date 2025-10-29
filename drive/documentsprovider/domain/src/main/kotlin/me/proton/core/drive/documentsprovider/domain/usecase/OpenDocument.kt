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
package me.proton.core.drive.documentsprovider.domain.usecase

import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.drivelink.download.domain.usecase.GetFile
import me.proton.core.drive.drivelink.upload.domain.usecase.UploadAlreadyCreatedFiles
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.upload.domain.usecase.CancelUploadFile
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenDocument @Inject constructor(
    private val withUploadFileLink: WithUploadFileLink,
    private val withDriveLinkFile: WithDriveLinkFile,
    private val withDriveLinkFolder: WithDriveLinkFolder,
    private val getFile: GetFile,
    private val getCacheFolder: GetCacheFolder,
    private val cancelUploadFile: CancelUploadFile,
    private val uploadFiles: UploadAlreadyCreatedFiles,
) {

    private val handlerThread = Handler(HandlerThread("OpenDocumentThread").apply {
        start()
    }.looper)

    private val mutexMap = ConcurrentHashMap<DocumentId, Mutex>()

    suspend operator fun invoke(
        documentId: DocumentId,
        mode: String?,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor =
        openUploadFileLink(documentId, mode, signal) ?: openDriveLinkFile(documentId, mode, signal)

    @Suppress("UNUSED_PARAMETER")
    private suspend fun openUploadFileLink(
        documentId: DocumentId,
        mode: String?,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor? =
        withUploadFileLink(documentId) { userId, uploadFileLink ->
            if (mode == null || mode.contains("w").not()) {
                throw UnsupportedOperationException("$mode is not supported, only 'w' for upload file node")
            }
            val file = uploadFileLink.cacheFolder()
            ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_WRITE_ONLY,
                handlerThread
            ) { exception ->
                if (exception != null) {
                    runBlocking { cancelUploadFile(uploadFileLink) }
                } else runBlocking {
                    withDriveLinkFolder(DocumentId(userId, uploadFileLink.parentLinkId)) { _, driveLink ->
                        uploadFiles(
                            folder = driveLink,
                            uploadFiles = listOf(uploadFileLink to Uri.fromFile(file).toString()),
                            shouldDeleteSource = true,
                        )
                    }
                }
            }
        }

    private suspend fun UploadFileLink.cacheFolder() = File(getCacheFolder(userId, volumeId.id, draftRevisionId), "tmp")
        .apply {
            if (!exists()) {
                createNewFile()
            }
        }

    private suspend fun openDriveLinkFile(
        documentId: DocumentId,
        mode: String?,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor = withDriveLinkFile(documentId) { _, driveLink ->
        if (mode == null || mode.contains("r").not()) {
            throw UnsupportedOperationException("$mode is not supported, only 'r'")
        }

        val state = mutexMap.getOrPut(documentId) { Mutex() }.withLock {
            getFile(driveLink).first { state ->
                when (state) {
                    is GetFile.State.Ready -> true
                    is GetFile.State.Error -> true
                    else -> signal?.isCanceled == true
                }
            }
        }
        require(state is GetFile.State.Ready)

        val file = File(state.uri.path!!)

        if (signal?.isCanceled == true) {
            file.delete()
            error("Request was canceled")
        }

        ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY,
            handlerThread,
        ) { error ->
            if (error != null) {
                CoreLogger.w(
                    LogTag.DOCUMENTS_PROVIDER,
                    error,
                    "An error occurred when file ${documentId.linkId?.id?.logId()} was used by another app"
                )
            }
        }
    }
}
