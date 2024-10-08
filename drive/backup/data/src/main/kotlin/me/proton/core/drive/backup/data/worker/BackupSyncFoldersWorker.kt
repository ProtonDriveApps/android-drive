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
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_FOLDER_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_SHARE_ID
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_UPLOAD_PRIORITY
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.SyncFolders
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.share.domain.entity.ShareId

@HiltWorker
class BackupSyncFoldersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncFolders: SyncFolders,
    private val addBackupError: AddBackupError,
) : CoroutineWorker(context, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val folderId = FolderId(shareId, requireNotNull(inputData.getString(KEY_FOLDER_ID)))
    private val uploadPriority = inputData.getLong(KEY_UPLOAD_PRIORITY, UploadFileLink.BACKUP_PRIORITY)

    override suspend fun doWork(): Result {
        syncFolders(folderId, uploadPriority).onFailure { error ->
            error.log(LogTag.BACKUP, "Cannot sync folders")
            addBackupError(folderId, error.toBackupError())
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        fun getWorkRequest(
            folderId: FolderId,
            uploadPriority: Long,
            tags: Collection<String> = emptyList(),
        ) = OneTimeWorkRequest.Builder(BackupSyncFoldersWorker::class.java)
            .setInputData(workDataOf(folderId, uploadPriority))
            .addTags(
                listOf(
                    folderId.userId.id,
                    folderId.id,
                    BackupManagerImpl.TAG
                ) + tags
            )
            .build()

        internal fun workDataOf(folderId: FolderId, uploadPriority: Long) = Data.Builder()
            .putString(KEY_USER_ID, folderId.userId.id)
            .putString(KEY_SHARE_ID, folderId.shareId.id)
            .putString(KEY_FOLDER_ID, folderId.id)
            .putLong(KEY_UPLOAD_PRIORITY, uploadPriority)
            .build()
    }
}
