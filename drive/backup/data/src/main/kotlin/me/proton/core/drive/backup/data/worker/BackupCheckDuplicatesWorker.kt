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
import me.proton.core.drive.backup.data.extension.toBackupError
import me.proton.core.drive.backup.data.manager.BackupManagerImpl
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_BUCKET_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.CheckDuplicates
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId

@HiltWorker
class BackupCheckDuplicatesWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkDuplicates: CheckDuplicates,
    private val addBackupError: AddBackupError,
) : CoroutineWorker(context, workerParams) {


    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val bucketId = inputData.getInt(KEY_BUCKET_ID, -1)

    override suspend fun doWork(): Result {
        inputData.folderId
            .onFailure { error ->
                error.log(LogTag.BACKUP, "Failed to get FolderId")
            }
            .onSuccess { folderId ->
                checkDuplicates(userId, BackupFolder(bucketId, folderId)).onFailure { error ->
                    error.log(LogTag.BACKUP, "Cannot check duplicates")
                    addBackupError(folderId, error.toBackupError())
                    return Result.failure()
                }
            }
        return Result.success()
    }

    private val Data.folderId: kotlin.Result<FolderId> get() = coRunCatching {
        FolderId(
            shareId = ShareId(userId, requireNotNull(getString(KEY_SHARE_ID))),
            id = requireNotNull(getString(KEY_FOLDER_ID)),
        )
    }

    companion object {
        fun getWorkRequest(
            backupFolder: BackupFolder,
            tags: Collection<String> = emptyList(),
        ): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(BackupCheckDuplicatesWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(workDataOf(backupFolder))
                .addTags(
                    listOf(
                        backupFolder.folderId.userId.id,
                        backupFolder.folderId.id,
                        BackupManagerImpl.TAG,
                    ) + tags
                )
                .build()
        }

        internal fun workDataOf(
            backupFolder: BackupFolder,
        ) = Data.Builder()
            .putString(KEY_USER_ID, backupFolder.folderId.userId.id)
            .putString(KEY_SHARE_ID, backupFolder.folderId.shareId.id)
            .putString(KEY_FOLDER_ID, backupFolder.folderId.id)
            .putInt(KEY_BUCKET_ID, backupFolder.bucketId)
            .build()
    }
}
