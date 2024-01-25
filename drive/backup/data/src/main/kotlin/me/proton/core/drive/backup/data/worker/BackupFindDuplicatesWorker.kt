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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.manager.BackupManagerImpl
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.FindDuplicates
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId

@HiltWorker
class BackupFindDuplicatesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val findDuplicates: FindDuplicates,
    private val configurationProvider: ConfigurationProvider,
    private val addBackupError: AddBackupError,
) : CoroutineWorker(context, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val folderId = FolderId(shareId, requireNotNull(inputData.getString(KEY_FOLDER_ID)))
    private val bucketId = inputData.getInt(WorkerKeys.KEY_BUCKET_ID, -1)
    private val timestamp = inputData.getLong(WorkerKeys.KEY_TIMESTAMP, -1).let { value ->
        if (value == -1L) {
            null
        } else {
            TimestampS(value)
        }
    }

    override suspend fun doWork(): Result {
        findDuplicates(BackupFolder(bucketId, folderId, timestamp)).onFailure { error ->
            val canRetry = runAttemptCount <= configurationProvider.maxApiAutoRetries
            val retryable = error.isRetryable
            error.log(
                tag = BACKUP,
                message = """
                    Cannot find duplicates for: ${folderId.id} retryable $retryable,
                    max retries reached ${!canRetry}
                """.trimIndent()
            )
            return if (canRetry && retryable) {
                Result.retry()
            } else {
                addBackupError(folderId, BackupError.Other(retryable))
                Result.failure()
            }
        }
        return Result.success()
    }

    companion object {
        fun getWorkRequest(
            backupFolder: BackupFolder,
            tags: Collection<String> = emptyList(),
        ) = OneTimeWorkRequest.Builder(BackupFindDuplicatesWorker::class.java)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(workDataOf(backupFolder))
            .addTags(listOf(
                backupFolder.folderId.userId.id,
                backupFolder.folderId.id,
                BackupManagerImpl.TAG
            ) + tags)
            .build()

        internal fun workDataOf(
            backupFolder: BackupFolder,
        ) = Data.Builder()
            .putString(KEY_USER_ID, backupFolder.folderId.userId.id)
            .putString(KEY_SHARE_ID, backupFolder.folderId.shareId.id)
            .putString(KEY_FOLDER_ID, backupFolder.folderId.id)
            .putInt(WorkerKeys.KEY_BUCKET_ID, backupFolder.bucketId)
            .putLong(WorkerKeys.KEY_TIMESTAMP, backupFolder.updateTime?.value ?: -1)
            .build()
    }
}
