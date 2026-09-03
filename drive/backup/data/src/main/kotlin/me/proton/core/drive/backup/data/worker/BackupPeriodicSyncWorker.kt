/*
 * Copyright (c) 2026 Proton AG.
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

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.manager.BackupManagerImpl
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.usecase.SyncFolders
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.util.kotlin.CoreLogger
import java.util.concurrent.TimeUnit

/**
 * Safety-net reconciliation, independent of [BackupFileWatcherWorker]'s content-URI trigger.
 */
@HiltWorker
class BackupPeriodicSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncFolders: SyncFolders,
) : CoroutineWorker(context, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))

    override suspend fun doWork(): Result {
        CoreLogger.i(BACKUP, "BackupPeriodicSyncWorker triggered for $userId")
        return syncFolders(userId, UploadFileLink.BACKUP_PRIORITY)
            .onSuccess { backupFolders ->
                CoreLogger.i(BACKUP, "BackupPeriodicSyncWorker synced ${backupFolders.size} folders")
            }
            .onFailure { error ->
                error.log(BACKUP, "BackupPeriodicSyncWorker cannot sync folders")
            }
            .fold(
                onSuccess = { Result.success() },
                // Periodic work reschedules itself regardless of this run's outcome, so there's no
                // need for Result.retry()'s backoff-driven extra attempt before the next interval.
                onFailure = { Result.failure() },
            )
    }

    companion object {
        fun uniqueWorkName(userId: UserId) = "backup-periodic-sync-${userId.id}"

        fun getWorkRequest(userId: UserId) =
            PeriodicWorkRequestBuilder<BackupPeriodicSyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .addTags(listOf(userId.id, BackupManagerImpl.TAG))
                .setInputData(workDataOf(userId))
                .build()

        internal fun workDataOf(userId: UserId) = Data.Builder()
            .putString(KEY_USER_ID, userId.id)
            .build()

        private const val SYNC_INTERVAL_HOURS = 1L
    }
}
