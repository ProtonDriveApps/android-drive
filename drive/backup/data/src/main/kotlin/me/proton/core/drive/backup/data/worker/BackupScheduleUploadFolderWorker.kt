/*
 * Copyright (c) 2023 Proton AG.
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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.extension.uniqueUploadWorkName
import me.proton.core.drive.backup.data.manager.BackupManagerImpl
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_DELAY
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import kotlin.time.Duration

@HiltWorker
class BackupScheduleUploadFolderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val workManager: WorkManager,
) : CoroutineWorker(context, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val folderId = FolderId(shareId, requireNotNull(inputData.getString(KEY_FOLDER_ID)))
    private val bucketId = inputData.getInt(WorkerKeys.KEY_BUCKET_ID, -1)
    private val delay = Duration.parseIsoString(requireNotNull(inputData.getString(KEY_DELAY)))

    override suspend fun doWork(): Result {
        val backupFolder = BackupFolder(bucketId, folderId)
        val existingWorkPolicy = if (delay == Duration.ZERO) {
            ExistingWorkPolicy.REPLACE
        } else {
            ExistingWorkPolicy.KEEP
        }
        workManager.enqueueUniqueWork(
            backupFolder.uniqueUploadWorkName(userId),
            existingWorkPolicy,
            BackupUploadFolderWorker.getWorkRequest(
                userId = userId,
                backupFolder = backupFolder,
                delay = delay,
                tags = tags,
            ),
        )
        return Result.success()
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            backupFolder: BackupFolder,
            delay: Duration,
            tags: Collection<String> = emptyList(),
        ): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(BackupScheduleUploadFolderWorker::class.java)
                .setInputData(workDataOf(userId, backupFolder, delay))
                .addTags(listOf(userId.id, BackupManagerImpl.TAG) + tags)
                .build()
        }

        internal fun workDataOf(
            userId: UserId,
            backupFolder: BackupFolder,
            delay: Duration,
        ) = Data.Builder()
            .putString(KEY_USER_ID, userId.id)
            .putString(KEY_SHARE_ID, backupFolder.folderId.shareId.id)
            .putString(KEY_FOLDER_ID, backupFolder.folderId.id)
            .putInt(WorkerKeys.KEY_BUCKET_ID, backupFolder.bucketId)
            .putString(KEY_DELAY, delay.toIsoString())
            .build()
    }
}
