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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.isConnectedToNetwork
import me.proton.core.drive.cryptobase.domain.exception.VerificationException
import me.proton.core.drive.drivelink.crypto.domain.usecase.DecryptLinkContent
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.download.domain.extension.isDownloaded
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.GetDownloadBlocks
import me.proton.core.drive.linkdownload.domain.usecase.GetDownloadState
import me.proton.core.drive.linkdownload.domain.usecase.RemoveDownloadState
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import javax.inject.Inject

class GetFile @Inject constructor(
    private val download: Download,
    private val cancelDownload: CancelDownload,
    private val getDownloadProgress: GetDownloadProgress,
    private val getDownloadState: GetDownloadState,
    private val decryptLinkContent: DecryptLinkContent,
    private val getCacheFolder: GetCacheFolder,
    private val getDriveLink: GetDriveLink,
    private val isConnectedToNetwork: isConnectedToNetwork,
    private val verifyDownloadedState: VerifyDownloadedState,
    private val getDownloadBlocks: GetDownloadBlocks,
    private val removeDownloadState: RemoveDownloadState,
) {
    operator fun invoke(
        driveLink: DriveLink.File,
        checkSignature: Boolean = true,
        retryable: Boolean = false,
    ): Flow<State> = flow {
        CoreLogger.d(LogTag.GET_FILE, "Getting file ${driveLink.id.id.logId()}")
        val cacheFolder = getCacheFolder(driveLink.userId, driveLink.volumeId.id, driveLink.activeRevisionId)
        val targetFile = File(cacheFolder, driveLink.id.id).apply { deleteOnExit() }
        if (targetFile.exists() && targetFile.length() != 0L) {
            CoreLogger.d(LogTag.GET_FILE, "Cache file for ${driveLink.id.id.logId()} already exists!")
            emit(State.Ready(Uri.fromFile(targetFile), driveLink.id))
            return@flow
        }
        CoreLogger.d(LogTag.GET_FILE, "Cache file for ${driveLink.id.id.logId()} doesn't exists")
        val verifiedDriveLink = verifyDownloadedState(driveLink)
            .onFailure { error ->
                CoreLogger.d(LogTag.GET_FILE, error, "Downloaded state verification failed")
            }.getOrThrow()
        val blocks = getDownloadBlocks(driveLink.id).getOrThrow()
        val downloadedDriveLink: DriveLink.File = if (!verifiedDriveLink.isDownloaded(blocks)) {
            removeDownloadState(verifiedDriveLink.link)
            CoreLogger.d(LogTag.GET_FILE, "File ${driveLink.id.id.logId()} is not downloaded yet, let's download it!")
            if (!retryable && !isConnectedToNetwork()) {
                CoreLogger.w(LogTag.GET_FILE, "Download ${driveLink.id.id.logId()} failed as it is not retryable and there is no network connection")
                return@flow emit(State.Error.NoConnection)
            }
            download(driveLink, retryable)
            try {
                waitForDownloadToFinish(driveLink)
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }
                CoreLogger.w(LogTag.GET_FILE, e, "There was an error while downloading ${driveLink.id.id.logId()}")
                return@flow emit(State.Error.Downloading(e))
            }
        } else driveLink
        CoreLogger.i(LogTag.GET_FILE, "File ${driveLink.id.id.logId()} is downloaded!")
        emit(State.Decrypting)

        CoreLogger.d(LogTag.GET_FILE, "Decrypting file ${driveLink.id.id.logId()}")
        decryptLinkContent(downloadedDriveLink, targetFile, checkSignature).onSuccess { file ->
            CoreLogger.i(LogTag.GET_FILE, "File ${driveLink.id.id.logId()} was successfully decrypted!")
            emit(State.Ready(Uri.fromFile(file), driveLink.id))
        }.onFailure { error ->
            CoreLogger.e(LogTag.GET_FILE, error,"There was an error decrypting file ${driveLink.id.id.logId()}")
            when (error) {
                is VerificationException -> emit(State.Error.VerifyingSignature(error))
                else -> emit(State.Error.Decrypting(error))
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<State>.waitForDownloadToFinish(
        driveLink: DriveLink.File
    ): DriveLink.File = try {
        driveLink.copy(
            downloadState = waitForDownloadedState(driveLink.id)
        )
    } catch (e: CancellationException) {
        if (!(driveLink.isMarkedAsOffline || driveLink.isAnyAncestorMarkedAsOffline)) {
            cancelDownload(driveLink)
        }
        throw e
    }

    private suspend fun FlowCollector<State>.waitForDownloadedState(fileId: FileId): DownloadState.Downloaded {
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
                DownloadState.Downloading -> {
                    if (!emittedDownloadingOnce) emittedDownloadingOnce = emitDownloading(fileId)
                    continue
                }
                is DownloadState.Downloaded -> return state
                DownloadState.Error -> throw IllegalStateException("File is not downloaded")
            }
        }
    }

    private suspend fun FlowCollector<State>.emitDownloading(fileId: FileId): Boolean =
        getDownloadProgress(getDriveLink(fileId).toResult().getOrThrow())?.let { progress ->
            emit(State.Downloading(progress))
            true
        } ?: false

    sealed class State {
        data class Downloading(val progress: Flow<Percentage>) : State()
        object Decrypting : State()
        data class Ready(val uri: Uri, val fileId: FileId) : State()
        sealed class Error : State() {
            object NoConnection : Error()
            object NotFound : Error()
            data class Downloading(val throwable: Throwable) : Error()
            data class VerifyingSignature(val throwable: Throwable) : Error()
            data class Decrypting(val throwable: Throwable) : Error()
        }
    }
}
