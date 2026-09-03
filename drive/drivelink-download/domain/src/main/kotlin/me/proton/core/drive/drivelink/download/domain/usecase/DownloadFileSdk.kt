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

package me.proton.core.drive.drivelink.download.domain.usecase

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import me.proton.android.drive.verifier.domain.exception.ContentDigestVerifierException
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.extension.requireIsInstance
import me.proton.core.drive.base.domain.extension.toHex
import me.proton.core.drive.base.domain.extension.toPercentage
import me.proton.core.drive.base.domain.formatter.DateTimeFormatter
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.DOWNLOAD
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetDownloadStagingTempFolder
import me.proton.core.drive.base.domain.usecase.IsRetryable
import me.proton.core.drive.base.domain.usecase.ReportError
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.crypto.domain.usecase.IntegrityMetricsNotifier
import me.proton.core.drive.drivelink.domain.usecase.GetVolumeType
import me.proton.core.drive.drivelink.download.domain.manager.DownloadSdkManager
import me.proton.core.drive.file.base.domain.extension.captureTime
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.decryptedFileName
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.revisionUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.provider.ProtonSdkClientProvider
import me.proton.core.drive.linkdownload.domain.manager.DownloadSpeedManager
import me.proton.core.drive.linkdownload.domain.usecase.RemoveSignatureVerificationFailed
import me.proton.core.drive.linkdownload.domain.usecase.SetSignatureVerificationFailed
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import me.proton.drive.sdk.DownloadAbortedException
import me.proton.drive.sdk.DownloadController
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.ProtonSdkError
import me.proton.drive.sdk.ProtonSdkError.ErrorDomain
import me.proton.drive.sdk.entity.FileDownloaderRequest
import me.proton.drive.sdk.entity.FileNode
import me.proton.drive.sdk.entity.Node
import me.proton.drive.sdk.entity.PhotosDownloaderRequest
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

class DownloadFileSdk @Inject constructor(
    private val downloadSdkManager: DownloadSdkManager,
    private val moveFileIfExists: MoveFileIfExists,
    private val getVolumeType: GetVolumeType,
    private val getDownloadStagingTempFolder: GetDownloadStagingTempFolder,
    private val setSignatureVerificationFailed: SetSignatureVerificationFailed,
    private val removeSignatureVerificationFailed: RemoveSignatureVerificationFailed,
    private val protonSdkClientProvider: ProtonSdkClientProvider,
    private val verifyDownloadedFile: VerifyDownloadedFile,
    private val integrityMetricsNotifier: IntegrityMetricsNotifier,
    private val configurationProvider: ConfigurationProvider,
    private val reportError: ReportError,
    private val dateTimeFormatter: DateTimeFormatter,
    private val downloadSpeedManager: DownloadSpeedManager,
    private val isRetryable: IsRetryable,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        revisionId: String,
        progress: MutableStateFlow<Percentage>,
    ): Result<Unit> = coroutineScope {
        var file: File?
        var tmpFile: File?
        coRunCatching {
            file = moveFileIfExists(volumeId, fileId, revisionId).getOrThrow()
            if (file.exists()) {
                CoreLogger.d(DOWNLOAD, "File already downloaded")
                return@coRunCatching
            }

            val volumeType = getVolumeType(fileId).getOrThrow()

            tmpFile = File(
                getDownloadStagingTempFolder(userId = fileId.userId, volumeId.id, revisionId),
                fileId.decryptedFileName,
            )
            when (volumeType) {
                Volume.Type.PHOTO -> {
                    downloadSdkManager.enqueuePhoto(
                        volumeId = volumeId,
                        fileId = fileId,
                        revisionId = revisionId,
                    ) { client ->
                        // TODO implement enqueue without timeout and noWaiting = true
                        withTimeout(configurationProvider.sdkQueueTimeout) {
                            client.downloader(
                                PhotosDownloaderRequest(
                                    nodeUid = fileId.nodeUid(volumeId),
                                    noWaiting = false,
                                )
                            )
                        }
                    }
                }

                Volume.Type.REGULAR -> {
                    downloadSdkManager.enqueueFile(
                        volumeId = volumeId,
                        fileId = fileId,
                        revisionId = revisionId,
                    ) { client ->
                        // TODO implement enqueue without timeout and noWaiting = true
                        withTimeout(configurationProvider.sdkQueueTimeout) {
                            client.downloader(
                                FileDownloaderRequest(
                                    revisionUid = fileId.revisionUid(
                                        volumeId = volumeId,
                                        revisionId = revisionId,
                                    ),
                                    noWaiting = false,
                                )
                            )
                        }
                    }
                }

                else -> error("Cannot download link for volume type: $volumeType")
            }

            val controller = downloadSdkManager.controller(
                volumeId = volumeId,
                fileId = fileId,
                revisionId = revisionId,
            ) { downloader ->
                downloader.downloadToStream(
                    coroutineScope = this,
                    channel = tmpFile.outputStream().channel
                )
            }
            val job = controller.progressFlow
                .filterNotNull()
                .mapWithPrevious { previous, current ->
                    val bytesDownloaded = current.bytesCompleted - (previous?.bytesCompleted ?: 0L)
                    current.toPercentage() to bytesDownloaded
                }
                .onEach { (percentage, bytesDownloaded) ->
                    progress.value = percentage
                    downloadSpeedManager.add(fileId.userId, bytesDownloaded)
                }
                .launchIn(this)
            controller.tryResume(this)
            downloadSpeedManager.resume()
            @Suppress("SwallowedException")
            try {
                removeSignatureVerificationFailed(fileId).getOrNull(
                    tag = DOWNLOAD,
                    message = "Failed to remove that signature verification failed"
                )
                controller.awaitCompletion()
            } catch (e: DownloadAbortedException) {
                val cause = e.cause
                if (controller.isNotVerified(e)) {
                    CoreLogger.w(
                        DOWNLOAD,
                        e,
                        "File downloaded but not verified, continuing"
                    )
                    setSignatureVerificationFailed(fileId).getOrNull(
                        tag = DOWNLOAD,
                        message = "Failed to set that signature verification failed"
                    )
                } else if (cause != null && isRetryable(cause)) {
                    CoreLogger.d(DOWNLOAD, e, "Download aborted with retryable cause")
                    downloadSdkManager.cancelController(volumeId, fileId, revisionId)
                    throw cause
                } else {
                    throw e
                }
            } finally {
                job.cancel()
            }

            Files.move(
                tmpFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            if (verifyDownloadedFile.isAllowed(userId = fileId.userId)) {
                var node: Node? = null
                var checksumVerified = false
                coRunCatching {
                    val nodeUid = fileId.nodeUid(volumeId)
                    node = protonSdkClientProvider.getOrCreate(
                        userId = fileId.userId,
                        volumeType = volumeType
                    ).getOrThrow().getNode(nodeUid)
                    checkNotNull(node) { "Cannot get node from: $nodeUid"}
                    val fileNode = requireIsInstance<FileNode>(node) { "Node must be a file from: $nodeUid"}
                    val contentDigests = fileNode.activeRevision.claimedDigests
                    checksumVerified = contentDigests?.sha1Verified == true
                    val sha1 = contentDigests?.sha1?.toHex().orEmpty()
                    verifyDownloadedFile(
                        claimed = sha1,
                        file = file,
                    ).getOrThrow()
                }
                    .onSuccess {
                        integrityMetricsNotifier.downloadVerifier(
                            fileSize = file.length().bytes,
                            isSuccess = true,
                            checksumVerified = checksumVerified,
                        )
                    }
                    .recoverCatching { error ->
                        if (error is ContentDigestVerifierException.Mismatch) {
                            val fileSize = file.length().bytes
                            integrityMetricsNotifier.downloadVerifier(
                                fileSize = fileSize,
                                isSuccess = false,
                                checksumVerified = checksumVerified,
                                throwable = error,
                            )
                            if (checksumVerified) {
                                file.delete()
                                throw error
                            } else {
                                reportError(
                                    tag = LogTag.DOWNLOAD,
                                    error = error,
                                    message = buildString {
                                        append("ContentDigestVerifierException.Mismatch ChecksumVerified=false, ")
                                        append("revisionId=${revisionId}, ")
                                        append("creationTime=${node?.creationTime?.captureTime(dateTimeFormatter)}")
                                    },
                                )
                            }
                        } else {
                            integrityMetricsNotifier.downloadVerifier(
                                fileSize = file.length().bytes,
                                isSuccess = true,
                                checksumVerified = checksumVerified,
                                throwable = error,
                            )
                        }
                    }
                    .getOrThrow()
            }
        }.onFailure { error ->
            reportError(
                tag = DOWNLOAD,
                error = error,
                message = "Cannot download ${fileId.id} (sdk)",
            )
        }
    }

    private suspend fun DownloadController.isNotVerified(error: DownloadAbortedException): Boolean {
        val cause = error.cause
        return cause is ProtonDriveSdkException && isNotVerified(cause.error)
    }

    private suspend fun DownloadController.isNotVerified(error: ProtonSdkError?): Boolean {
        return error != null
                && error.domain == ErrorDomain.DataIntegrity
                && isDownloadCompleteWithVerificationIssue()
    }
}
