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
package me.proton.core.drive.upload.data.manager

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.getLong
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.log.LogTag.NOTIFICATION
import me.proton.core.drive.base.domain.log.LogTag.UPLOAD
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.GetUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinksPaged
import me.proton.core.drive.linkupload.domain.usecase.RemoveAllUploadFileLinks
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.upload.data.extension.uniqueUploadWorkName
import me.proton.core.drive.upload.data.usecase.BroadcastFilesBeingUploaded
import me.proton.core.drive.upload.data.worker.CreateUploadFileLinkWorker
import me.proton.core.drive.upload.data.worker.UploadCleanupWorker
import me.proton.core.drive.upload.data.worker.UploadEventWorker
import me.proton.core.drive.upload.data.worker.UploadThrottleWorker
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_SIZE
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import me.proton.core.drive.upload.domain.usecase.AnnounceUploadEvent
import me.proton.core.drive.upload.domain.usecase.CreateUploadFile
import me.proton.core.drive.upload.domain.usecase.RemoveUploadFileAndAnnounceCancelled
import me.proton.core.drive.upload.domain.usecase.UpdateUploadFileInfo
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class UploadWorkManagerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val workManager: WorkManager,
    private val getUploadBlocks: GetUploadBlocks,
    private val getUploadFileLinks: GetUploadFileLinksPaged,
    private val broadcastMessages: BroadcastMessages,
    private val broadcastFilesBeingUploaded: BroadcastFilesBeingUploaded,
    private val createUploadFile: CreateUploadFile,
    private val updateUploadFileInfo: UpdateUploadFileInfo,
    private val updateUploadState: UpdateUploadState,
    private val announceUploadEvent: AnnounceUploadEvent,
    private val configurationProvider: ConfigurationProvider,
    private val removeUploadFileAndAnnounceCancelled: RemoveUploadFileAndAnnounceCancelled,
    private val removeAllUploadFileLinks: RemoveAllUploadFileLinks,
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
        cacheOption: CacheOption,
        shouldDeleteSource: Boolean,
        networkTypeProviderType: NetworkTypeProviderType,
        shouldAnnounceEvent: Boolean,
        priority: Long,
        shouldBroadcastErrorMessage: Boolean,
    ): List<UploadFileLink> {
        val uploadFileLinks = createUploadFile(
            userId = userId,
            volumeId = volumeId,
            parentId = folderId,
            uploadFileDescriptions = uriStrings.map { uriString ->
                UploadFileDescription(uri = uriString, properties = null)
            },
            shouldDeleteSourceUri = shouldDeleteSource,
            networkTypeProviderType = networkTypeProviderType,
            shouldAnnounceEvent = shouldAnnounceEvent,
            cacheOption = cacheOption,
            priority = priority,
            shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
        )
            .onFailure { error ->
                error.log(
                    tag = UPLOAD,
                    message = "Create upload file failed"
                )
            }
            .onSuccess { uploadFileLinks ->
                if (shouldAnnounceEvent) {
                    uploadFileLinks.forEach { uploadFileLink ->
                        announceUploadEvent(uploadFileLink, newUploadEvent(uploadFileLink.id))
                    }
                }
            }.getOrNull().orEmpty()
        if(uploadFileLinks.isNotEmpty()) {
            workManager.enqueueUpload(userId, shouldAnnounceEvent)
        }
        return uploadFileLinks
    }

    override suspend fun upload(
        uploadBulk: UploadBulk,
        folder: Folder,
        showPreparingUpload: Boolean,
        showFilesBeingUploaded: Boolean,
    ) {
        workManager.enqueueUniqueWork(
            uploadBulk.userId.uniqueUploadBulkWorkName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            CreateUploadFileLinkWorker.getWorkRequest(
                uploadBulk,
                folder.name,
                showFilesBeingUploaded,
            ),
        )
        if (showPreparingUpload) {
            broadcastMessages(
                userId = uploadBulk.userId,
                message = appContext.getString(I18N.string.files_upload_preparing),
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
        announceUploadEvent(uploadFileLinkId, newUploadEvent(uploadFileLinkId))
        updateUploadState(uploadFileLinkId, UploadState.UNPROCESSED)
        workManager.enqueueUpload(userId)
    }

    override suspend fun cancel(uploadFileLink: UploadFileLink): Unit = with (uploadFileLink) {
        workManager.cancelAllWorkByTag(id.uniqueUploadWorkName).await()
        if (!linkId.isNullOrEmpty()) {
            workManager.enqueue(
                UploadCleanupWorker.getWorkRequest(
                    userId = userId,
                    uploadFileLinkId = id,
                    isCancelled = true
                )
            ).await()
        } else {
            removeUploadFileAndAnnounceCancelled(uploadFileLink).getOrThrow()
        }
    }

    override suspend fun cancelAll(userId: UserId) = withContext(Job() + Dispatchers.IO) {
        workManager.cancelUniqueWork(userId.uniqueUploadEventWorkName).await()
        workManager.cancelUniqueWork(userId.uniqueUploadBulkWorkName).await()
        removeAllUploadFileLinks(userId, UploadState.UNPROCESSED)
        getUploadFileLinks(userId).forEach { uploadFileLink ->
            cancel(uploadFileLink)
        }
    }

    override suspend fun waitUploadEventWorkerToFinish(userId: UserId) {
        workManager.getWorkInfosForUniqueWorkLiveData(userId.uniqueUploadEventWorkName)
            .asFlow()
            .transform { workInfoList ->
                workInfoList?.firstOrNull()?.state?.let { state ->
                    if (state.isFinished) {
                        emit(Unit)
                    }
                } ?: emit(Unit)
            }
            .first()
    }

    override suspend fun cancelAllByShare(userId: UserId, shareId: ShareId) {
        workManager.cancelUniqueWork(userId.uniqueUploadBulkWorkName).await()
        removeAllUploadFileLinks(userId, shareId, UploadState.UNPROCESSED)
        getUploadFileLinks(userId, shareId).forEach { uploadFileLink ->
            cancel(uploadFileLink)
        }
    }

    override suspend fun cancelAllByFolder(userId: UserId, folderId: FolderId) {
        workManager.cancelUniqueWork(userId.uniqueUploadBulkWorkName).await()
        removeAllUploadFileLinks(userId, folderId, UploadState.UNPROCESSED)
        getUploadFileLinks(userId, folderId).forEach { uploadFileLink ->
            cancel(uploadFileLink)
        }
    }

    override suspend fun cancelAllByFolderAndUris(folderId: FolderId, uriStrings: List<String>) {
        workManager.cancelUniqueWork(folderId.userId.uniqueUploadBulkWorkName).await()
        removeAllUploadFileLinks(folderId, uriStrings, UploadState.UNPROCESSED)
        getUploadFileLinks(folderId, uriStrings).forEach { uploadFileLink ->
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
                I18N.plurals.files_upload_being_uploaded_notification,
                uriStrings.size,
                uriStrings.size,
                folder.name,
            ),
            type = BroadcastMessage.Type.INFO,
        )

    override fun broadcastFilesBeingUploaded(
        folder: Folder,
        uriStrings: List<String>,
        uploadFileLinks: List<UploadFileLink>,
    ) =
        broadcastFilesBeingUploaded(
            userId = folder.userId,
            folderName = folder.name,
            uriStringSize = uriStrings.size,
            uploadFileLinksSize = uploadFileLinks.size,
        )

    override fun broadcastUploadLimitReached(userId: UserId) =
        broadcastMessages(
            userId = userId,
            message = appContext.getString(
                I18N.string.files_upload_limit_reached,
                configurationProvider.uploadLimitThreshold,
            ),
            type = BroadcastMessage.Type.WARNING,
        )

    private fun newUploadEvent(uploadFileLinkId: Long) = Event.Upload(
        state = Event.Upload.UploadState.NEW_UPLOAD,
        uploadFileLinkId = uploadFileLinkId,
        percentage = Percentage(0),
        shouldShow = true,
    )

    internal companion object {
        const val TAG_UPLOAD_WORKER = "upload_worker"
    }
}

internal val UserId.uniqueUploadEventWorkName: String get() = "upload_notification_event=$id"
internal val UserId.uniqueUploadThrottleWorkName: String get() = "upload_throttle=$id"
internal val UserId.uniqueUploadBulkWorkName: String get() = "upload_bulk=$id"

internal suspend fun WorkManager.enqueueUpload(
    userId: UserId,
    shouldAnnounceEvent: Boolean = true,
    tags: Set<String> = emptySet(),
) {
    if (shouldAnnounceEvent) {
        enqueueUniqueWork(
            userId.uniqueUploadEventWorkName,
            ExistingWorkPolicy.REPLACE,
            UploadEventWorker.getWorkRequest(userId, tags.toList())
        )
    } else {
        CoreLogger.d(
            NOTIFICATION,
            "Ignoring enqueue of UploadEventWorker, no announce needed"
        )
    }
    enqueueUniqueWork(
        userId.uniqueUploadThrottleWorkName,
        ExistingWorkPolicy.KEEP,
        UploadThrottleWorker.getWorkRequest(userId, tags.toList())
    ).await()
}
