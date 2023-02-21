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
package me.proton.core.drive.upload.data.manager

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.workmanager.getLong
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.log
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.presentation.extension.getName
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.GetUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinks
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.usecase.AnnounceEvent
import me.proton.core.drive.upload.data.extension.uniqueUploadWorkName
import me.proton.core.drive.upload.data.worker.CreateUploadFileLinkWorker
import me.proton.core.drive.upload.data.worker.UploadCleanupWorker
import me.proton.core.drive.upload.data.worker.UploadNotificationEventWorker
import me.proton.core.drive.upload.data.worker.UploadThrottleWorker
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_SIZE
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import me.proton.core.drive.upload.domain.usecase.CreateUploadFile
import me.proton.core.drive.upload.domain.usecase.UpdateUploadFileInfo
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import me.proton.core.drive.base.presentation.R as BasePresentation

class UploadWorkManagerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val workManager: WorkManager,
    private val getUploadBlocks: GetUploadBlocks,
    private val getUploadFileLinks: GetUploadFileLinks,
    private val broadcastMessages: BroadcastMessages,
    private val createUploadFile: CreateUploadFile,
    private val updateUploadFileInfo: UpdateUploadFileInfo,
    private val updateUploadState: UpdateUploadState,
    private val announceEvent: AnnounceEvent,
    private val configurationProvider: ConfigurationProvider,
) : UploadWorkManager {


    /**
     * https://www.websequencediagrams.com/
     *
    opt If not created before
    FileUploadWorker->+CreateNewFileWorker: State.CreatingNewFile
    CreateNewFileWorker->-FileUploadWorker:
    end

    FileUploadWorker->+EncryptBlocksWorker: State.EncryptingBlocks
    EncryptBlocksWorker->-FileUploadWorker:

    FileUploadWorker->GetBlocksUploadUrlWorker: State.GettingUploadLinks
    loop for each blocks:
    GetBlocksUploadUrlWorker->+BlockUploadWorker: State.UploadingBlocks
    BlockUploadWorker->-GetBlocksUploadUrlWorker:
    end
    GetBlocksUploadUrlWorker->+UpdateRevisionWorker: State.UpdatingRevision
    UpdateRevisionWorker->-GetBlocksUploadUrlWorker:
    GetBlocksUploadUrlWorker->+UploadSuccessCleanupWorker: State.Cleanup
    UploadSuccessCleanupWorker->-GetBlocksUploadUrlWorker:
    GetBlocksUploadUrlWorker->FileUploadWorker:
     *
     */
    override suspend fun upload(
        userId: UserId,
        volumeId: VolumeId,
        folderId: FolderId,
        uriStrings: List<String>,
        shouldDeleteSource: Boolean
    ) {
        createUploadFile(
            userId = userId,
            volumeId = volumeId,
            parentId = folderId,
            uriStrings = uriStrings,
            shouldDeleteSourceUri = shouldDeleteSource,
        )
            .onFailure { error ->
                error.log(
                    tag = LogTag.UPLOAD,
                    message = "Create upload file failed"
                )
            }
            .onSuccess { uploadFileLinks ->
                uploadFileLinks.forEach { uploadFileLink ->
                    uploadFileLink.id.announceEvent(userId)
                }
            }
        workManager.enqueueUpload(userId)
    }

    override suspend fun upload(
        uploadBulk: UploadBulk,
        folder: Folder,
        silently: Boolean,
    ) {
        workManager.enqueueUniqueWork(
            uploadBulk.userId.uniqueUploadBulkWorkName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            CreateUploadFileLinkWorker.getWorkRequest(uploadBulk, folder.getName(appContext)),
        )
        if (!silently) {
            broadcastMessages(
                userId = uploadBulk.userId,
                message = appContext.getString(BasePresentation.string.files_upload_preparing),
                type = BroadcastMessage.Type.INFO,
            )
        }
    }

    override suspend fun uploadAlreadyCreated(
        userId: UserId,
        uploadFileLinkId: Long,
        uriString: String,
        shouldDeleteSource: Boolean,
    ) {
        updateUploadFileInfo(uploadFileLinkId, uriString, shouldDeleteSource)
        uploadFileLinkId.announceEvent(userId)
        updateUploadState(uploadFileLinkId, UploadState.UNPROCESSED)
        workManager.enqueueUpload(userId)
    }

    override fun cancel(uploadFileLink: UploadFileLink): Unit = with (uploadFileLink) {
        workManager.cancelAllWorkByTag(id.uniqueUploadWorkName)
        workManager.enqueue(
            UploadCleanupWorker.getWorkRequest(
                userId = userId,
                uploadFileLinkId = id,
                isCancelled = true
            )
        )
    }

    override suspend fun cancelAll(userId: UserId) {
        getUploadFileLinks(userId).first().forEach { uploadFileLink ->
            cancel(uploadFileLink)
        }
    }

    override fun getProgressFlow(uploadFileLink: UploadFileLink): Flow<Percentage>? {
        return if (uploadFileLink.state != UploadState.UPLOADING_BLOCKS) {
            null
        } else {
            //workManager.pruneWork()
            flow {
                getUploadBlocks(uploadFileLink)
                    .getOrNull()
                    ?.sumOf { uploadBlock -> uploadBlock.size.value }
                    ?.takeIf { uploadFileLinkSize -> uploadFileLinkSize > 0L }
                    ?.let { uploadFileLinkSize ->
                        emitAll(
                            workManager.getWorkInfosByTagLiveData(uploadFileLink.id.uniqueUploadWorkName)
                                .asFlow()
                                .transform { workInfos ->
                                    emit(
                                        Percentage(
                                            workInfos
                                                .sumOf { workInfo -> workInfo.getLong(KEY_SIZE) }
                                                .toFloat()
                                                .div(uploadFileLinkSize)
                                        )
                                    )
                                }
                        )
                    }
            }
        }
    }

    override fun broadcastFilesBeingUploaded(folder: Folder, uriStrings: List<String>) =
        broadcastMessages(
            userId = folder.userId,
            message = appContext.resources.getQuantityString(
                BasePresentation.plurals.files_upload_being_uploaded_notification,
                uriStrings.size,
                uriStrings.size,
                folder.getName(appContext),
            ),
            type = BroadcastMessage.Type.INFO,
        )

    override fun broadcastUploadLimitReached(userId: UserId) =
        broadcastMessages(
            userId = userId,
            message = appContext.getString(
                BasePresentation.string.files_upload_limit_reached,
                configurationProvider.uploadLimitThreshold,
            ),
            type = BroadcastMessage.Type.WARNING,
        )

    private suspend fun Long.announceEvent(userId: UserId) =
        announceEvent(
            userId = userId,
            notificationEvent = NotificationEvent.Upload(
                state = NotificationEvent.Upload.UploadState.NEW_UPLOAD,
                uploadFileLinkId = this,
                percentage = Percentage(0),
            )
        )
}

internal val UserId.uniqueUploadNotificationEventWorkName: String get() = "upload_notification_event=$id"
internal val UserId.uniqueUploadThrottleWorkName: String get() = "upload_throttle=$id"
internal val UserId.uniqueUploadBulkWorkName: String get() = "upload_bulk=$id"

internal fun WorkManager.enqueueUpload(userId: UserId) {
    enqueueUniqueWork(
        userId.uniqueUploadNotificationEventWorkName,
        ExistingWorkPolicy.REPLACE,
        UploadNotificationEventWorker.getWorkRequest(userId)
    )
    enqueueUniqueWork(
        userId.uniqueUploadThrottleWorkName,
        ExistingWorkPolicy.KEEP,
        UploadThrottleWorker.getWorkRequest(userId)
    )
}
