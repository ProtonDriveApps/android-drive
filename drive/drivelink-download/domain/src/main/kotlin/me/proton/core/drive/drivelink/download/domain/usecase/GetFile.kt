/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.drivelink.download.domain.usecase

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.usecase.isConnectedToNetwork
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.decryptedFileName
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.file.base.domain.extension.toXAttr
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.GetDownloadState
import me.proton.core.drive.linkdownload.domain.usecase.HasSignatureVerificationFailed
import me.proton.core.drive.linkdownload.domain.usecase.RemoveDownloadState
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import javax.inject.Inject

class GetFile @Inject constructor(
    private val download: Download,
    private val cancelDownload: CancelDownload,
    private val getDownloadProgress: GetDownloadProgress,
    private val getDownloadState: GetDownloadState,
    private val getCacheFolder: GetCacheFolder,
    private val getPermanentFolder: GetPermanentFolder,
    private val getDriveLink: GetDriveLink,
    private val isConnectedToNetwork: isConnectedToNetwork,
    private val removeDownloadState: RemoveDownloadState,
    private val isLinkOrAnyAncestorMarkedAsOffline: IsLinkOrAnyAncestorMarkedAsOffline,
    private val hasSignatureVerificationFailed: HasSignatureVerificationFailed,
) {
    operator fun invoke(
        driveLink: DriveLink.File,
        checkSignature: Boolean = true,
        retryable: Boolean = false,
    ): Flow<State> = flow {
        CoreLogger.d(LogTag.GET_FILE, "Getting file ${driveLink.id.id.logId()}")

        val cacheFile = File(
            getCacheFolder(
                userId = driveLink.userId,
                volumeId = driveLink.volumeId.id,
                revisionId = driveLink.activeRevisionId,
            ),
            driveLink.decryptedFileName,
        )

        val cacheFileValid = driveLink.checkFile(cacheFile)
            .getOrNull(LogTag.GET_FILE, "Cannot check file: ${driveLink.id.id.logId()}")
        if (cacheFileValid == true) {
            CoreLogger.d(LogTag.GET_FILE, "Cache file for ${driveLink.id.id.logId()} already exists!")
            checkFileSignature(driveLink, cacheFile, checkSignature)
            return@flow
        }

        val permanentFile = File(
            getPermanentFolder(
                userId = driveLink.userId,
                volumeId = driveLink.volumeId.id,
                revisionId = driveLink.activeRevisionId,
            ),
            driveLink.decryptedFileName,
        )

        val permanentFileValid = driveLink.checkFile(permanentFile)
            .getOrNull(LogTag.GET_FILE, "Cannot check file: ${driveLink.id.id.logId()}")
        if (permanentFileValid == true) {
            CoreLogger.d(LogTag.GET_FILE, "Permanent file for ${driveLink.id.id.logId()} already exists!")
            checkFileSignature(driveLink, permanentFile, checkSignature)
            return@flow
        }
        CoreLogger.d(LogTag.GET_FILE, "File for ${driveLink.id.id.logId()} doesn't exists")
        removeDownloadState(driveLink.link)
        CoreLogger.d(LogTag.GET_FILE, "File ${driveLink.id.id.logId()} is not downloaded yet, let's download it!")
        if (!retryable && !isConnectedToNetwork()) {
            CoreLogger.w(LogTag.GET_FILE, "Download ${driveLink.id.id} failed as it is not retryable and there is no network connection")
            return@flow emit(State.Error.NoConnection)
        }
        download(driveLink, retryable).getOrThrow()
        try {
            waitForDownloadToFinish(driveLink)
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            CoreLogger.w(LogTag.GET_FILE, e, "There was an error while downloading ${driveLink.id.id}")
            return@flow emit(State.Error.Downloading(e))
        }
        CoreLogger.i(LogTag.GET_FILE, "File ${driveLink.id.id.logId()} is downloaded!")
        val parentFolder = if (isLinkOrAnyAncestorMarkedAsOffline(driveLink.id)) {
            getPermanentFolder(
                userId = driveLink.userId,
                volumeId = driveLink.volumeId.id,
                revisionId = driveLink.activeRevisionId,
            )
        } else {
            getCacheFolder(
                userId = driveLink.userId,
                volumeId = driveLink.volumeId.id,
                revisionId = driveLink.activeRevisionId,
            )
        }
        val targetFile = File(parentFolder, driveLink.decryptedFileName)
        if (checkSignature && hasSignatureVerificationFailed(driveLink.id).getOrDefault(false)) {
            emit(State.Error.VerifyingSignature(RuntimeException("Throwable is not available")))
        } else {
            emit(State.Ready(Uri.fromFile(targetFile)))
        }
        checkFileSignature(driveLink, targetFile, checkSignature)
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<State>.checkFileSignature(
        driveLink: DriveLink.File,
        file: File,
        checkSignature: Boolean,
    ) {
        if (checkSignature && hasSignatureVerificationFailed(driveLink.id).getOrDefault(false)) {
            emit(State.Error.VerifyingSignature(RuntimeException("Throwable is not available")))
        } else {
            emit(State.Ready(Uri.fromFile(file)))
        }
    }

    private suspend fun FlowCollector<State>.waitForDownloadToFinish(
        driveLink: DriveLink.File
    ): DriveLink.File = try {
        driveLink.copy(
            downloadState = waitForReadyState(driveLink.id)
        )
    } catch (e: CancellationException) {
        if (!(driveLink.isMarkedAsOffline || driveLink.isAnyAncestorMarkedAsOffline)) {
            CoreLogger.d(LogTag.GET_FILE, "Downloading ${driveLink.id.id.logId()} will be cancelled due to CancellationException")
            withContext(NonCancellable) {
                cancelDownload(driveLink)
            }
        }
        throw e
    }

    private suspend fun FlowCollector<State>.waitForReadyState(fileId: FileId): DownloadState.Ready {
        CoreLogger.d(LogTag.GET_FILE, "Waiting for file ${fileId.id.logId()} to be downloaded!")
        var emittedDownloadingOnce = false
        var previousState: DownloadState? = DownloadState.Error
        while (true) {
            val state = getDownloadState(fileId).first { result ->
                when (result) {
                    is DataResult.Processing -> false
                    is DataResult.Success,
                    is DataResult.Error -> true
                }
            }.toResult().getOrThrow()
            if (previousState?.javaClass != state?.javaClass) {
                CoreLogger.d(
                    tag = LogTag.GET_FILE,
                    message = "Current download state for file ${fileId.id.logId()} is ${state?.javaClass?.simpleName}",
                )
                previousState = state
            }
            when (state) {
                null -> continue
                DownloadState.Downloading,
                is DownloadState.Downloaded -> {
                    if (!emittedDownloadingOnce) emittedDownloadingOnce = emitDownloading(fileId)
                    continue
                }
                is DownloadState.Ready -> return state
                DownloadState.Error -> throw IllegalStateException("File is not downloaded")
            }
        }
    }

    private suspend fun FlowCollector<State>.emitDownloading(fileId: FileId): Boolean =
        getDownloadProgress(
            getDriveLink(fileId).toResult().getOrThrow()
        )?.let { progress ->
            emit(State.Downloading(progress))
            true
        } ?: false

    private fun DriveLink.File.checkFile(file: File) : Result<Boolean> = coRunCatching {
        if (!file.exists()) {
            return@coRunCatching false
        }
        val xAttr = cryptoXAttr.value?.toXAttr()?.getOrNull()
        val fileSize = xAttr?.common?.size

        if (fileSize == null) {
            CoreLogger.w(
                LogTag.GET_FILE,
                "Cannot found real size for ${this.id.id}"
            )
            return@coRunCatching file.length() != 0L
        }
        val delta = fileSize - file.length()
        if (delta != 0L) {
            CoreLogger.w(
                LogTag.GET_FILE,
                "Unexpected size of existing file for ${this.id.id}, delta=${delta}"
            )
            file.delete()
            false
        } else {
            true
        }
    }

    sealed class State {
        data class Downloading(val progress: Flow<Percentage>) : State()
        object Decrypting : State()
        data class Ready(val uri: Uri) : State()
        sealed class Error : State() {
            object NoConnection : Error()
            object NotFound : Error()
            data class Downloading(val throwable: Throwable) : Error()
            data class VerifyingSignature(val throwable: Throwable) : Error()
            data class Decrypting(val throwable: Throwable) : Error()
        }
    }
}
