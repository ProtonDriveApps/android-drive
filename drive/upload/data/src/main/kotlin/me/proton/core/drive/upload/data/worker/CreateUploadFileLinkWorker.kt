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
package me.proton.core.drive.upload.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.usecase.DeleteUploadBulk
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.usecase.AnnounceEvent
import me.proton.core.drive.upload.data.manager.enqueueUpload
import me.proton.core.drive.upload.domain.usecase.CreateUploadFile
import me.proton.core.drive.upload.domain.usecase.HasEnoughAvailableSpace
import me.proton.core.drive.i18n.R as I18N

@HiltWorker
class CreateUploadFileLinkWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val createUploadFile: CreateUploadFile,
    private val deleteUploadBulk: DeleteUploadBulk,
    private val hasEnoughAvailableSpace: HasEnoughAvailableSpace,
    private val announceEvent: AnnounceEvent,
    private val broadcastMessages: BroadcastMessages,
    private val workManager: WorkManager,
) : CoroutineWorker(appContext, workerParams) {
    private val uploadBulkId: Long = inputData.getLong(WorkerKeys.KEY_UPLOAD_BULK_ID, -1L)
    private val folderName = requireNotNull(inputData.getString(WorkerKeys.KEY_FOLDER_NAME)) { "Folder name is required" }

    override suspend fun doWork(): Result {
        deleteUploadBulk(uploadBulkId).getOrNull()?.let { uploadBulk ->
            with(uploadBulk) {
                val hasEnoughSpace = hasEnoughAvailableSpace(userId, uriStrings) { needed ->
                    announceEvent(
                        userId = userId,
                        notificationEvent = NotificationEvent.StorageFull(needed)
                    )
                }
                if (!hasEnoughSpace) return@let
                createUploadFile(
                    userId = userId,
                    volumeId = volumeId,
                    parentId = parentLinkId,
                    uriStrings = uriStrings,
                    shouldDeleteSourceUri = shouldDeleteSourceUri,
                )
                    .onFailure { error ->
                        error.log(
                            tag = LogTag.UPLOAD,
                            message = "Create upload file failed"
                        )
                    }
                    .onSuccess { uploadFileLinks ->
                        uploadFileLinks.forEach { uploadFileLink ->
                            announceEvent(
                                userId = userId,
                                notificationEvent = NotificationEvent.Upload(
                                    state = NotificationEvent.Upload.UploadState.NEW_UPLOAD,
                                    uploadFileLinkId = uploadFileLink.id,
                                    percentage = Percentage(0)
                                )
                            )
                        }
                    }
                    .getOrNull()
                    ?.let {
                        broadcastMessages(
                            userId = userId,
                            message = appContext.resources.getQuantityString(
                                I18N.plurals.files_upload_being_uploaded_notification,
                                uriStrings.size,
                                uriStrings.size,
                                folderName,
                            ),
                            type = BroadcastMessage.Type.INFO,
                        )
                        workManager.enqueueUpload(userId)
                        return Result.success()
                    }
            }
        }
        return Result.failure()
    }

    companion object {
        fun getWorkRequest(
            uploadBulk: UploadBulk,
            folderName: String,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(CreateUploadFileLinkWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putLong(WorkerKeys.KEY_UPLOAD_BULK_ID, uploadBulk.id)
                        .putString(WorkerKeys.KEY_FOLDER_NAME, folderName)
                        .build()
                )
                .addTags(listOf(uploadBulk.userId.id) + tags)
                .build()
    }
}
