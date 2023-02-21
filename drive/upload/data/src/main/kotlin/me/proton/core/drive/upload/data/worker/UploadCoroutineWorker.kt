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
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.logDefaultMessage
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.upload.data.exception.UploadCleanupException
import me.proton.core.drive.upload.data.extension.getDefaultMessage
import me.proton.core.drive.upload.data.extension.log
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.util.kotlin.CoreLogger
import java.io.IOException

@ExperimentalCoroutinesApi
abstract class UploadCoroutineWorker(
    private val appContext: Context,
    workerParams: WorkerParameters,
    protected val workManager: WorkManager,
    protected val broadcastMessages: BroadcastMessages,
    private val getUploadFileLink: GetUploadFileLink,
    protected val configurationProvider: ConfigurationProvider,
) : CoroutineWorker(appContext, workerParams) {

    protected val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)) { "User id is required" })
    protected val uploadFileLinkId: Long = inputData.getLong(KEY_UPLOAD_FILE_LINK_ID, -1L)

    override suspend fun doWork(): Result {
        return try {
            val uploadFileLink = getUploadFileLink(uploadFileLinkId).toResult().getOrThrow()
            doUploadWork(uploadFileLink)
        } catch (e: CancellationException) {
            CoreLogger.d(logTag(), "Retrying due to cancellation exception")
            Result.retry()
        } catch (e: NoSuchElementException) {
            CoreLogger.d(logTag(), "Cannot find upload file link")
            Result.failure()
        } catch (e: Exception) {
            when (e) {
                is UploadCleanupException,
                is IOException,
                is CryptoException -> {
                    workManager.enqueue(
                        UploadCleanupWorker.getWorkRequest(userId, uploadFileLinkId)
                    )
                    broadcastMessages(
                        userId = userId,
                        message = applicationContext.getString(
                            BasePresentation.string.files_upload_failure_with_description,
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
                    Result.failure()
                }
                else -> throw e
            }
        }
    }

    protected open fun logTag() = with(LogTag.UploadTag) {
        uploadFileLinkId.logTag()
    }

    abstract suspend fun doUploadWork(uploadFileLink: UploadFileLink): Result
}
