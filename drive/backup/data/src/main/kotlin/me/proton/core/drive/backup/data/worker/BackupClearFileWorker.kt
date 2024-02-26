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
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.extension.toBackupError
import me.proton.core.drive.backup.data.manager.BackupManagerImpl
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_FILE_URI
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.MarkAsCompleted
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId

@HiltWorker
class BackupClearFileWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val markAsCompleted: MarkAsCompleted,
    private val addBackupError: AddBackupError,
) : CoroutineWorker(context, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val uriString = requireNotNull(inputData.getString(KEY_FILE_URI))

    override suspend fun doWork(): Result {
        inputData.folderId
            .onFailure { error ->
                error.log(LogTag.BACKUP, "Failed to get FolderId")
            }
            .onSuccess { folderId ->
                markAsCompleted(folderId, uriString).onFailure { error ->
                    error.log(LogTag.BACKUP, "Cannot mark file as completed with uri: $uriString")
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
            uriString: String,
            tags: Collection<String> = emptyList(),
        ): OneTimeWorkRequest {
            return OneTimeWorkRequest.Builder(BackupClearFileWorker::class.java)
                .setInputData(workDataOf(backupFolder, uriString))
                .addTags(
                    listOf(
                        backupFolder.folderId.userId.id,
                        backupFolder.folderId.id,
                        BackupManagerImpl.TAG
                    ) + tags
                )
                .build()
        }

        internal fun workDataOf(
            backupFolder: BackupFolder,
            uriString: String,
        ) = Data.Builder()
            .putString(KEY_USER_ID, backupFolder.folderId.userId.id)
            .putString(KEY_SHARE_ID, backupFolder.folderId.shareId.id)
            .putString(KEY_FOLDER_ID, backupFolder.folderId.id)
            .putString(KEY_FILE_URI, uriString)
            .build()
    }
}
