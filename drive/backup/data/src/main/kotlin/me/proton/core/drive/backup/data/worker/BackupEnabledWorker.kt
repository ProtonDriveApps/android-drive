/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.backup.data.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.usecase.GetAllFolders
import me.proton.core.drive.backup.domain.usecase.GetBackupStatus
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.worker.data.usecase.TransferDataNotifier
import me.proton.core.util.kotlin.CoreLogger
import kotlin.time.Duration.Companion.minutes

@HiltWorker
class BackupEnabledWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getBackupStatus: GetBackupStatus,
    private val getAllFolders: GetAllFolders,
    private val transferDataNotifier: TransferDataNotifier
) : CoroutineWorker(appContext, workerParams) {

    private val userId =
        UserId(requireNotNull(inputData.getString(WorkerKeys.KEY_USER_ID)) { "User id is required" })

    private val transferDataNotification: Pair<NotificationId, Notification> = transferDataNotifier(
        userId = userId,
        event = Event.TransferData,
    )

    override suspend fun doWork(): Result {
        CoreLogger.d(LogTag.BACKUP, "BackupEnabledWorker started")
        setForeground(createForegroundInfo())
        try {
            while (true) {
                delay(1.minutes)
                if (areAllFoldersFinished(userId)) {
                    break
                }
            }
        } finally {
            transferDataNotifier.dismissNotification(transferDataNotification.first)
        }
        CoreLogger.d(LogTag.BACKUP, "BackupEnabledWorker ended")
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo()

    private fun createForegroundInfo(): ForegroundInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                transferDataNotification.first.id,
                transferDataNotification.second,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(transferDataNotification.first.id, transferDataNotification.second)
        }

    private suspend fun areAllFoldersFinished(userId: UserId): Boolean =
        getAllFolders(userId)
            .getOrNull(LogTag.BACKUP, "Getting all backup folders failed")
            ?.map { folder -> folder.folderId }
            ?.toSet()
            ?.all { folderId -> getBackupStatus(folderId).firstOrNull()?.isFinished ?: true }
            ?: true

    private val BackupStatus.isFinished: Boolean get() = this is BackupStatus.Complete ||
            this is BackupStatus.Failed ||
            this is BackupStatus.Uncompleted

    companion object {
        fun getWorkRequest(
            userId: UserId,
            tags: List<String> = emptyList(),
        ): OneTimeWorkRequest =
            OneTimeWorkRequest.Builder(BackupEnabledWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setInputData(
                    Data.Builder()
                        .putString(WorkerKeys.KEY_USER_ID, userId.id)
                        .build()
                )
                .addTags(listOf(userId.id) + tags)
                .build()

        fun getUniqueWorkName(userId: UserId) = "BackupEnabledWorker.${userId.id.logId()}"
    }
}
