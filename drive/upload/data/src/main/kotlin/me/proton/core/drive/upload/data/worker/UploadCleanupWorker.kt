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
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.usecase.AnnounceEvent
import me.proton.core.drive.upload.data.extension.logTag
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_IS_CANCELLED
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.usecase.GetBlockFolder
import me.proton.core.drive.upload.domain.usecase.RemoveUploadFile
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.TimeUnit

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class)
class UploadCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    private val getBlockFolder: GetBlockFolder,
    private val removeUploadFile: RemoveUploadFile,
    private val announceEvent: AnnounceEvent,
    configurationProvider: ConfigurationProvider,
) : UploadCoroutineWorker(
    appContext = appContext,
    workerParams = workerParams,
    workManager = workManager,
    broadcastMessages = broadcastMessages,
    getUploadFileLink = getUploadFileLink,
    configurationProvider = configurationProvider,
) {
    private val isCancelled = inputData.getBoolean(KEY_IS_CANCELLED, false)

    override suspend fun doUploadWork(uploadFileLink: UploadFileLink): Result {
        try {
            getBlockFolder(userId, uploadFileLink)
                .onFailure { error -> error.log(uploadFileLink.logTag()) }
                .getOrNull()?.deleteRecursively()
            removeUploadFile(uploadFileLink)
            announceEvent(
                userId = userId,
                notificationEvent = NotificationEvent.Upload(
                    state = if (isCancelled) {
                        NotificationEvent.Upload.UploadState.UPLOAD_CANCELLED
                    } else {
                        NotificationEvent.Upload.UploadState.UPLOAD_FAILED
                    },
                    uploadFileLinkId = uploadFileLink.id,
                    percentage = Percentage(0)
                )
            )
        } finally {
            uploadFileLink.deleteOnServer()
        }
        return Result.success()
    }

    private fun UploadFileLink.deleteOnServer() {
        val linkId = linkId
        if (!linkId.isNullOrEmpty()) {
            CoreLogger.d(logTag(), "Upload cleanup worker continue with delete on server")
            workManager.enqueue(
                DeleteFileLinkWorker.getWorkRequest(userId, shareId.id, parentLinkId.id, linkId)
            )
        }
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uploadFileLinkId: Long,
            isCancelled: Boolean = false,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(UploadCleanupWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putLong(KEY_UPLOAD_FILE_LINK_ID, uploadFileLinkId)
                        .putBoolean(KEY_IS_CANCELLED, isCancelled)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }
}
