/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.upload.data.worker

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.work.WorkInfo.Companion.STOP_REASON_NOT_STOPPED
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.data.extension.logDefaultMessage
import me.proton.core.drive.base.data.extension.stopReasonAsEnum
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.extension.parentLinkId
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.upload.data.exception.UploadCleanupException
import me.proton.core.drive.upload.data.exception.UploadWorkerException
import me.proton.core.drive.upload.data.extension.getDefaultMessage
import me.proton.core.drive.upload.data.extension.log
import me.proton.core.drive.upload.data.extension.toEventUploadReason
import me.proton.core.drive.upload.data.extension.uniqueUploadWorkName
import me.proton.core.drive.upload.data.provider.NetworkTypeProvider
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.manager.post
import me.proton.core.drive.upload.domain.provider.FileProvider
import me.proton.core.drive.worker.data.LimitedRetryCoroutineWorker
import me.proton.core.drive.worker.domain.entity.Constraint
import me.proton.core.drive.worker.domain.usecase.WorkerRuns
import me.proton.core.util.kotlin.CoreLogger
import java.io.IOException
import me.proton.core.drive.i18n.R as I18N

@ExperimentalCoroutinesApi
abstract class UploadCoroutineWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    protected val workManager: WorkManager,
    protected val broadcastMessages: BroadcastMessages,
    private val getUploadFileLink: GetUploadFileLink,
    private val uploadErrorManager: UploadErrorManager,
    protected val configurationProvider: ConfigurationProvider,
    workerRuns: WorkerRuns,
) : LimitedRetryCoroutineWorker(appContext, workerParams, workerRuns) {

    override val userId = UserId(
        requireNotNull(inputData.getString(KEY_USER_ID)) { "User id is required" }
    )
    protected val uploadFileLinkId: Long = inputData.getLong(KEY_UPLOAD_FILE_LINK_ID, -1L)
    override val logTag: String get() = logTag()

    private var isCancelled: Boolean? = null

    override suspend fun doLimitedRetryWork(): Result {
        var uploadFileLink: UploadFileLink? = null
        return try {
            uploadFileLink = getUploadFileLink(uploadFileLinkId).toResult().getOrThrow()
            currentUploadFileLink.value = uploadFileLink
            doLimitedRetryUploadWork(uploadFileLink)
        } catch (e: CancellationException) {
            CoreLogger.d(
                logTag(),
                "Cancellation exception in ${javaClass.simpleName}"
            )
            val error = if (SDK_INT >= VERSION_CODES.S && stopReason != STOP_REASON_NOT_STOPPED) {
                    UploadWorkerException(
                        name = javaClass.simpleName,
                        stopReason = stopReasonAsEnum,
                        cause = e
                    )
                } else {
                    UploadWorkerException(
                        name = javaClass.simpleName,
                        cause = e,
                    )
                }
            if (blockedBy == null) {
                uploadFileLink?.post(error)
            }

            throw e
        } catch (e: NoSuchElementException) {
            uploadFileLink?.run {
                post(e)
            }
            CoreLogger.d(logTag(), "Cannot find upload file link")
            Result.failure()
        } catch (e: Exception) {
            uploadFileLink?.run {
                post(e)
            }
            // DataResult.toResult() always wraps its cause in a new RuntimeException, so a
            // missing link never surfaces as the NoSuchElementException caught above.
            if (uploadFileLink == null && e.cause is NoSuchElementException) {
                CoreLogger.d(logTag(), "Cannot find upload file link")
                Result.failure()
            } else {
                when (e) {
                    is UploadCleanupException,
                    is IOException,
                    is CryptoException,
                    -> {
                        val reason = e.getCause().toEventUploadReason()
                        workManager.enqueue(
                            UploadCleanupWorker.getWorkRequest(
                                userId = userId,
                                uploadFileLinkId = uploadFileLinkId,
                                reason = reason,
                                isCancelled = isCancelled ?: false,
                                tags = listOf(uploadFileLinkId.uniqueUploadWorkName),
                            )
                        )
                        uploadFileLink.broadcastMessages(e)
                        Result.failure()
                    }

                    else -> throw e
                }
            }
        } finally {
            if (SDK_INT >= VERSION_CODES.S && stopReason != STOP_REASON_NOT_STOPPED) {
                CoreLogger.d(
                    tag = logTag(),
                    message = "$stopReasonAsEnum (id=$id, runAttemptCount=$runAttemptCount)",
                )
            }
        }
    }

    override suspend fun onRetriesExhausted() {
        CoreLogger.w(logTag(), "Retries exhausted in ${javaClass.simpleName}, cleaning up")
        workManager.enqueue(
            UploadCleanupWorker.getWorkRequest(
                userId = userId,
                uploadFileLinkId = uploadFileLinkId,
                reason = Event.Upload.Reason.ERROR_OTHER,
                isCancelled = isCancelled ?: false,
                tags = listOf(uploadFileLinkId.uniqueUploadWorkName),
            )
        )
    }

    private fun UploadFileLink?.broadcastMessages(e: Exception) {
        if (this == null || this.shouldBroadcastErrorMessage) {
            broadcastMessages(
                userId = userId,
                message = applicationContext.getString(
                    I18N.string.files_upload_failure_with_description,
                    if (e is UploadCleanupException) e.fileName else "",
                    when (e) {
                        is UploadCleanupException -> e.log(logTag()).getDefaultMessage(
                            context = appContext,
                            useExceptionMessage = configurationProvider.useExceptionMessage,
                        )

                        else -> e.logDefaultMessage(appContext, logTag())
                    }
                ),
                type = BroadcastMessage.Type.ERROR
            )
        }
    }

    private fun UploadFileLink.post(error: Throwable) {
        val cause = error.getCause()
        uploadErrorManager.post(
            uploadFileLink = this,
            tags = tags,
            throwable = cause,
        )
    }

    private fun Throwable.getCause() = if (this is UploadCleanupException) {
        cause ?: this
    } else {
        this
    }

    protected open fun logTag() = with(LogTag.UploadTag) {
        uploadFileLinkId.logTag()
    }

    protected fun UploadFileLink.logWorkState(message: String = "") {
        val workerId = this@UploadCoroutineWorker.id
        val workerName = this@UploadCoroutineWorker.javaClass.simpleName
        CoreLogger.d(
            logTag(),
            "$workerName($runAttemptCount) $message: $uriString [$workerId]"
        )
    }

    protected fun setUploadAsCancelled() {
        isCancelled = true
    }

    protected fun UploadFileLink.deleteSourceFile(fileProvider: FileProvider) = coRunCatching {
        if (shouldDeleteSourceUri) {
            uriString?.let {
                val sourceFile = fileProvider.getFile(it)
                if (sourceFile.exists()) {
                    sourceFile.delete()
                }
            }
        }
    }

    private val currentUploadFileLink = MutableStateFlow<UploadFileLink?>(null)

    abstract suspend fun doLimitedRetryUploadWork(uploadFileLink: UploadFileLink): Result

    protected fun networkNotAllowed(
        networkTypeProviders: Map<NetworkTypeProviderType, NetworkTypeProvider>,
    ): Flow<Constraint.Runtime> = flow {
        val uploadFileLink = currentUploadFileLink.filterNotNull().first()
        val networkTypeProvider = requireNotNull(networkTypeProviders[uploadFileLink.networkTypeProviderType])
        emitAll(
            networkTypeProvider.isSatisfied(uploadFileLink.parentLinkId)
                .filter { isSatisfied -> !isSatisfied }
                .map { Constraint.Runtime.NetworkNotAllowed }
        )
    }

    protected suspend fun awaitAllowedNetwork(
        networkTypeProviders: Map<NetworkTypeProviderType, NetworkTypeProvider>,
        uploadFileLink: UploadFileLink,
    ) {
        val networkTypeProvider = requireNotNull(networkTypeProviders[uploadFileLink.networkTypeProviderType])
        networkTypeProvider.isSatisfied(uploadFileLink.parentLinkId).firstOrNull { isSatisfied -> isSatisfied }
            ?: awaitCancellation()
    }
}
