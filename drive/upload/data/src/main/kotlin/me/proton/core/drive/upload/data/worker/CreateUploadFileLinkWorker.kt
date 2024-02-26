/*
 * Copyright (c) 2022-2024 Proton AG.
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
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.log.LogTag.UPLOAD_BULK
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.usecase.DeleteUploadBulk
import me.proton.core.drive.linkupload.domain.usecase.GetUploadBulk
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.upload.data.manager.enqueueUpload
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_FOLDER_NAME
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_SHOW_FILES_BEING_UPLOADED
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_BULK_ID
import me.proton.core.drive.upload.domain.usecase.AnnounceUploadEvent
import me.proton.core.drive.upload.domain.usecase.CreateUploadFile
import me.proton.core.drive.upload.domain.usecase.HasEnoughAvailableSpace
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.drive.i18n.R as I18N

@HiltWorker
@Suppress("LongParameterList")
class CreateUploadFileLinkWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val createUploadFile: CreateUploadFile,
    private val getUploadBulk: GetUploadBulk,
    private val deleteUploadBulk: DeleteUploadBulk,
    private val hasEnoughAvailableSpace: HasEnoughAvailableSpace,
    private val announceEvent: AnnounceEvent,
    private val announceUploadEvent: AnnounceUploadEvent,
    private val broadcastMessages: BroadcastMessages,
    private val workManager: WorkManager,
    private val configurationProvider: ConfigurationProvider,
) : CoroutineWorker(appContext, workerParams) {
    private val uploadBulkId: Long = inputData.getLong(KEY_UPLOAD_BULK_ID, -1L)
    private val folderName = requireNotNull(inputData.getString(KEY_FOLDER_NAME)) { "Folder name is required" }
    private val showFilesBeingUploaded = inputData.getBoolean(KEY_SHOW_FILES_BEING_UPLOADED, true)

    override suspend fun doWork(): Result = coRunCatching {
        with(getUploadBulk(uploadBulkId).getOrThrow()) {
            val uploadFileLinksSize = uploadFileDescriptions.size
            uploadFileDescriptions.chunked(configurationProvider.dbPageSize).forEach { uploadFileDescriptions ->
                logWorkState("Adding ${uploadFileDescriptions.size} files to upload")
                val hasEnoughSpace = hasEnoughAvailableSpace(
                    userId = userId,
                    uriStrings = uploadFileDescriptions.map { description -> description.uri }) { needed ->
                    announceEvent(
                        userId = userId,
                        event = Event.StorageFull(needed)
                    )
                }
                if (!hasEnoughSpace) {
                    logWorkState("Cannot upload files, not enough space")
                    return@with
                }
                val uploadFileLinks = createUploadFile(
                    userId = userId,
                    volumeId = volumeId,
                    parentId = parentLinkId,
                    uploadFileDescriptions = uploadFileDescriptions,
                    shouldDeleteSourceUri = shouldDeleteSourceUri,
                    networkTypeProviderType = networkTypeProviderType,
                    shouldAnnounceEvent = shouldAnnounceEvent,
                    cacheOption = cacheOption,
                    priority = priority,
                    shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
                ).onFailure { error ->
                    error.log(UPLOAD_BULK, "Cannot create upload files for ${uploadFileDescriptions.size} files")
                }.getOrThrow()
                deleteUploadBulk(
                    uploadBulkId = uploadBulkId,
                    uriStrings = uploadFileDescriptions.map { description -> description.uri },
                ).onFailure { error ->
                    error.log(UPLOAD_BULK, "Cannot delete upload bulk for ${uploadFileDescriptions.size} files")
                }.getOrThrow()
                logWorkState("${uploadFileLinks.size} files were added to upload")
                uploadFileLinks.forEach { uploadFileLink ->
                    announceUploadEvent(
                        uploadFileLink = uploadFileLink,
                        uploadEvent = Event.Upload(
                            state = Event.Upload.UploadState.NEW_UPLOAD,
                            uploadFileLinkId = uploadFileLink.id,
                            percentage = Percentage(0),
                            shouldShow = uploadFileLink.shouldAnnounceEvent,
                        )
                    )
                }
            }
            if (showFilesBeingUploaded) {
                broadcastMessages(
                    userId = userId,
                    message = appContext.resources.getQuantityString(
                        I18N.plurals.files_upload_being_uploaded_notification,
                        uploadFileLinksSize,
                        uploadFileLinksSize,
                        folderName,
                    ),
                    type = BroadcastMessage.Type.INFO,
                )
            }
            workManager.enqueueUpload(userId, shouldAnnounceEvent, tags)
            deleteUploadBulk(uploadBulkId).onFailure { error ->
                error.log(UPLOAD_BULK, "Cannot delete upload bulk for: $uploadBulkId")
            }.getOrThrow()
        }
    }.fold(
        onSuccess = {
            Result.success()
        },
        onFailure = { error ->
            error.log(logTag(), "Cannot upload files from bulk")
            Result.failure()
        }
    )

    private fun logWorkState(message: String = "") {
        val workerId = this@CreateUploadFileLinkWorker.id
        val workerName = this@CreateUploadFileLinkWorker.javaClass.simpleName
        CoreLogger.d(logTag(), "$workerName($runAttemptCount) $message [$workerId]")
    }

    private fun logTag() = "$UPLOAD_BULK.$uploadBulkId"

    companion object {
        fun getWorkRequest(
            uploadBulk: UploadBulk,
            folderName: String,
            showFilesBeingUploaded: Boolean,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(CreateUploadFileLinkWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putLong(KEY_UPLOAD_BULK_ID, uploadBulk.id)
                        .putString(KEY_FOLDER_NAME, folderName)
                        .putBoolean(KEY_SHOW_FILES_BEING_UPLOADED, showFilesBeingUploaded)
                        .build()
                )
                .addTags(listOf(uploadBulk.userId.id) + tags)
                .build()
    }
}
