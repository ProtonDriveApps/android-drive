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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.verifier.domain.usecase.CleanupVerifier
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.upload.data.extension.logTag
import me.proton.core.drive.upload.data.manager.uniqueUploadThrottleWorkName
import me.proton.core.drive.upload.data.provider.NetworkTypeProvider
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_IS_CANCELLED
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_REASON
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_UPLOAD_FILE_LINK_ID
import me.proton.core.drive.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.usecase.AnnounceUploadEvent
import me.proton.core.drive.upload.domain.usecase.GetBlockFolder
import me.proton.core.drive.upload.domain.usecase.RemoveUploadFile
import me.proton.core.drive.worker.domain.usecase.CanRun
import me.proton.core.drive.worker.domain.usecase.Done
import me.proton.core.drive.worker.domain.usecase.Run
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.deserializeOrNull
import me.proton.core.util.kotlin.serialize
import java.util.concurrent.TimeUnit

@HiltWorker
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList")
class UploadCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workManager: WorkManager,
    broadcastMessages: BroadcastMessages,
    getUploadFileLink: GetUploadFileLink,
    uploadErrorManager: UploadErrorManager,
    private val getBlockFolder: GetBlockFolder,
    private val removeUploadFile: RemoveUploadFile,
    private val announceUploadEvent: AnnounceUploadEvent,
    private val networkTypeProviders: @JvmSuppressWildcards Map<NetworkTypeProviderType, NetworkTypeProvider>,
    private val cleanupVerifier: CleanupVerifier,
    configurationProvider: ConfigurationProvider,
    canRun: CanRun,
    run: Run,
    done: Done,
) : UploadCoroutineWorker(
    appContext = appContext,
    workerParams = workerParams,
    workManager = workManager,
    broadcastMessages = broadcastMessages,
    getUploadFileLink = getUploadFileLink,
    uploadErrorManager = uploadErrorManager,
    configurationProvider = configurationProvider,
    canRun = canRun,
    run = run,
    done = done,
) {
    private val isCancelled = inputData.getBoolean(KEY_IS_CANCELLED, false)
    private val reason: Event.Upload.Reason = inputData.getString(KEY_REASON)
        ?.deserializeOrNull() ?: Event.Upload.Reason.ERROR_OTHER

    override suspend fun doLimitedRetryUploadWork(uploadFileLink: UploadFileLink): Result {
        CoreLogger.d(
            uploadFileLink.logTag(),
            "UploadCleanupWorker clean ${uploadFileLink.uriString}",
        )
        workManager.enqueueUniqueWork(
            userId.uniqueUploadThrottleWorkName,
            ExistingWorkPolicy.KEEP,
            UploadThrottleWorker.getWorkRequest(userId)
        )
        try {
            announceUploadEvent(
                uploadFileLink = uploadFileLink,
                uploadEvent = Event.Upload(
                    state = if (isCancelled) {
                        Event.Upload.UploadState.UPLOAD_CANCELLED
                    } else {
                        Event.Upload.UploadState.UPLOAD_FAILED
                    },
                    uploadFileLinkId = uploadFileLink.id,
                    percentage = Percentage(0),
                    shouldShow = uploadFileLink.shouldAnnounceEvent,
                    reason = reason,
                )
            )
            getBlockFolder(userId, uploadFileLink)
                .onFailure { error -> error.log(uploadFileLink.logTag()) }
                .getOrNull()?.deleteRecursively()
            removeUploadFile(uploadFileLink)
            uploadFileLink.linkId?.let { linkId ->
                cleanupVerifier(
                    userId = userId,
                    shareId = uploadFileLink.shareId.id,
                    linkId = linkId,
                    revisionId = uploadFileLink.draftRevisionId,
                )
            }
        } finally {
            uploadFileLink.deleteOnServer()
        }
        return Result.success()
    }

    private fun UploadFileLink.deleteOnServer() {
        val linkId = linkId
        if (!linkId.isNullOrEmpty()) {
            CoreLogger.d(logTag(), "Upload cleanup worker continue with delete on server")
            val networkType = requireNotNull(networkTypeProviders[networkTypeProviderType]).get()
            workManager.enqueue(
                DeleteFileLinkWorker.getWorkRequest(
                    userId = userId,
                    shareId = shareId.id,
                    folderId = parentLinkId.id,
                    uploadFileId = linkId,
                    networkType = networkType,
                )
            )
        }
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            uploadFileLinkId: Long,
            isCancelled: Boolean = false,
            reason: Event.Upload.Reason? = null,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(UploadCleanupWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, userId.id)
                        .putLong(KEY_UPLOAD_FILE_LINK_ID, uploadFileLinkId)
                        .putBoolean(KEY_IS_CANCELLED, isCancelled)
                        .putString(KEY_REASON, reason?.serialize())
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTags(listOf(userId.id) + tags)
                .build()
    }
}
