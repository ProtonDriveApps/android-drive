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
package me.proton.core.drive.upload.data.worker

import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.extension.logDefaultMessage
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.upload.data.exception.UploadCleanupException
import me.proton.core.drive.upload.data.extension.getDefaultMessage
import me.proton.core.drive.upload.data.extension.log
import me.proton.core.drive.upload.data.extension.toEventUploadReason
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.manager.post
import me.proton.core.drive.worker.data.LimitedRetryCoroutineWorker
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
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
    canRun: CanRun,
    run: Run,
    done: Done,
) : LimitedRetryCoroutineWorker(appContext, workerParams, canRun, run, done) {

    override val userId = UserId(
        requireNotNull(inputData.getString(KEY_USER_ID)) { "User id is required" }
    )
    protected val uploadFileLinkId: Long = inputData.getLong(KEY_UPLOAD_FILE_LINK_ID, -1L)
    override val logTag: String get() = logTag()

    override suspend fun doLimitedRetryWork(): Result {
        var uploadFileLink: UploadFileLink? = null
        return try {
            uploadFileLink = getUploadFileLink(uploadFileLinkId).toResult().getOrThrow()
            doLimitedRetryUploadWork(uploadFileLink)
        } catch (e: CancellationException) {
            CoreLogger.d(logTag(), "Retrying due to cancellation exception in ${javaClass.simpleName}")
            Result.retry()
        } catch (e: NoSuchElementException) {
            uploadFileLink?.post(e)
            CoreLogger.d(logTag(), "Cannot find upload file link")
            Result.failure()
        } catch (e: Exception) {
            uploadFileLink?.post(e)
            when (e) {
                is UploadCleanupException,
                is IOException,
                is CryptoException,
                -> {
                    workManager.enqueue(
                        UploadCleanupWorker.getWorkRequest(
                            userId,
                            uploadFileLinkId,
                            reason = e.getCause().toEventUploadReason()
                        )
                    )
                    uploadFileLink.broadcastMessages(e)
                    Result.failure()
                }

                else -> throw e
            }
        }
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

    private suspend fun UploadFileLink.post(error: Throwable) {
        val cause = error.getCause()
        uploadErrorManager.post(this, tags, cause)
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

    abstract suspend fun doLimitedRetryUploadWork(uploadFileLink: UploadFileLink): Result
}
