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
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_TIMESTAMP
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_UPLOAD_PRIORITY
import me.proton.core.drive.backup.data.worker.WorkerKeys.KEY_USER_ID
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.ScanFolder
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.data.workmanager.addTags
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.util.kotlin.CoreLogger

@HiltWorker
class BackupScanFolderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scanFolder: ScanFolder,
    private val addBackupError: AddBackupError,
) : CoroutineWorker(context, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(KEY_USER_ID)))
    private val shareId = ShareId(userId, requireNotNull(inputData.getString(KEY_SHARE_ID)))
    private val folderId = FolderId(shareId, requireNotNull(inputData.getString(KEY_FOLDER_ID)))
    private val bucketId = inputData.getInt(WorkerKeys.KEY_BUCKET_ID, -1)
    private val timestamp = inputData.getLong(KEY_TIMESTAMP, -1).let { value ->
        if (value == -1L) {
            null
        } else {
            TimestampS(value)
        }
    }
    private val uploadPriority =
        inputData.getLong(KEY_UPLOAD_PRIORITY, UploadFileLink.BACKUP_PRIORITY)

    override suspend fun doWork(): Result {
        scanFolder(
            userId,
            BackupFolder(bucketId, folderId, timestamp),
            uploadPriority
        ).onFailure { error ->
            error.log(BACKUP, "Cannot scan bucket: $bucketId")
            addBackupError(userId, error.toBackupError())
            return Result.failure()
        }.onSuccess { files ->
            val min = files.minByOrNull { file -> file.date }?.date
            val max = files.maxByOrNull { file -> file.date }?.date
            CoreLogger.d(
                BACKUP,
                """
                    Scan found ${files.size} files for folder:
                    $bucketId after $timestamp (min:$min, max: $max),
                    priority $uploadPriority
                """.trimIndent()
            )
        }
        return Result.success()
    }

    companion object {
        fun getWorkRequest(
            userId: UserId,
            backupFolder: BackupFolder,
            uploadPriority: Long,
            tags: Collection<String> = emptyList(),
        ) = OneTimeWorkRequest.Builder(BackupScanFolderWorker::class.java)
            .setInputData(workDataOf(userId, backupFolder, uploadPriority))
            .addTags(listOf(userId.id, BackupManagerImpl.TAG) + tags)
            .build()

        internal fun workDataOf(
            userId: UserId,
            backupFolder: BackupFolder,
            uploadPriority: Long,
        ) = Data.Builder()
            .putString(KEY_USER_ID, userId.id)
            .putString(KEY_SHARE_ID, backupFolder.folderId.shareId.id)
            .putString(KEY_FOLDER_ID, backupFolder.folderId.id)
            .putInt(WorkerKeys.KEY_BUCKET_ID, backupFolder.bucketId)
            .putLong(KEY_TIMESTAMP, backupFolder.updateTime?.value ?: -1)
            .putLong(KEY_UPLOAD_PRIORITY, uploadPriority)
            .build()
    }
}
