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

package me.proton.core.drive.drivelink.upload.data.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.upload.data.usecase.CreateFolderTreeAndScheduleUpload
import me.proton.core.drive.drivelink.upload.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.drivelink.upload.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.drivelink.upload.data.worker.WorkerKeys.KEY_SHOULD_BROADCAST_MESSAGE
import me.proton.core.drive.drivelink.upload.data.worker.WorkerKeys.KEY_URI
import me.proton.core.drive.drivelink.upload.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.upload.data.extension.getDefaultMessage
import me.proton.core.drive.worker.data.LimitedRetryCoroutineWorker
import me.proton.core.drive.worker.data.usecase.TransferDataNotifier
import me.proton.core.drive.worker.domain.usecase.WorkerRuns
import me.proton.core.util.kotlin.CoreLogger
import kotlin.Result as KotlinResult

@HiltWorker
class UploadFolderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    workerRuns: WorkerRuns,
    private val transferDataNotifier: TransferDataNotifier,
    private val createFolderTreeAndScheduleUpload: CreateFolderTreeAndScheduleUpload,
    private val getDecryptedDriveLink: GetDecryptedDriveLink,
    private val broadcastMessages: BroadcastMessages,
) : LimitedRetryCoroutineWorker(appContext, workerParams, workerRuns) {
    override val userId = UserId(
        requireNotNull(inputData.getString(KEY_USER_ID)) { "User id is required" }
    )
    override val logTag: String get() = LogTag.UPLOAD
    private val folderId = FolderId(
        ShareId(
            userId,
            id = requireNotNull(inputData.getString(KEY_SHARE_ID)) { "Share id is required" },
        ),
        id = requireNotNull(inputData.getString(KEY_FOLDER_ID)) { "Folder id is required" },
    )
    private val uriString = requireNotNull(inputData.getString(KEY_URI)) { "Uri is required" }
    private val shouldBroadcastMessage = inputData.getBoolean(KEY_SHOULD_BROADCAST_MESSAGE, false)

    private val transferDataNotification: KotlinResult<Pair<NotificationId, Notification>> = transferDataNotifier(
        userId = userId,
        event = Event.TransferData,
    )

    override suspend fun doLimitedRetryWork(): Result {
        CoreLogger.i(logTag, "Start upload folder worker for uri=$uriString")
        setForeground()
        return try {
            createFolderTreeAndScheduleUpload(
                folder = getDecryptedDriveLink(userId, folderId).toResult().getOrThrow(),
                uriString = uriString,
                shouldBroadcastMessage = shouldBroadcastMessage,
            ).fold(
                onSuccess = { Result.success() },
                onFailure = { error ->
                    val isRetryable = error.isRetryable && canRetry()
                    error.log(LogTag.UPLOAD, "UploadFolder failed, retryable=$isRetryable")
                    if (isRetryable) {
                        Result.retry()
                    } else {
                        broadcastMessages(
                            userId = userId,
                            message = error.getDefaultMessage(appContext, false),
                            type = BroadcastMessage.Type.ERROR,
                        )
                        Result.failure()
                    }
                }
            )
        } finally {
            transferDataNotification.getOrNull(LogTag.UPLOAD)
                ?.first
                ?.let { notificationId ->
                    transferDataNotifier.dismissNotification(notificationId)
                }
            CoreLogger.i(logTag, "Finished upload folder worker for uri=$uriString")
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo().getOrThrow()

    private fun createForegroundInfo(): KotlinResult<ForegroundInfo> = coRunCatching {
        val transferNotification = transferDataNotification.getOrThrow()
        ForegroundInfo(
            transferNotification.first.id,
            transferNotification.second,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private suspend fun setForeground() {
        createForegroundInfo()
            .getOrNull(logTag, "Failed creating foreground info")
            ?.let { foregroundInfo ->
                try {
                    setForeground(foregroundInfo)
                } catch (e: IllegalStateException) {
                    e.log(logTag, "Failed to set foreground")
                }
            }
    }

    companion object {
        fun getWorkRequest(
            folderId: FolderId,
            uriString: String,
            shouldBroadcastMessage: Boolean,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(UploadFolderWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(KEY_USER_ID, folderId.userId.id)
                        .putString(KEY_SHARE_ID, folderId.shareId.id)
                        .putString(KEY_FOLDER_ID, folderId.id)
                        .putString(KEY_URI, uriString)
                        .putBoolean(KEY_SHOULD_BROADCAST_MESSAGE, shouldBroadcastMessage)
                        .build()
                )
                .addTags(listOf(folderId.userId.id) + tags)
                .build()

        fun getUniqueWorkName(uriString: String) = "UploadFolderWorker.$uriString"
    }
}
