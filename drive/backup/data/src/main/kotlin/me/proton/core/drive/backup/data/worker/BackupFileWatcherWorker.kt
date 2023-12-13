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
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.repository.ContextScanFilesRepository
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.util.kotlin.CoreLogger

@HiltWorker
@RequiresApi(Build.VERSION_CODES.N)
class BackupFileWatcherWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupManager: BackupManager,
    private val workManager: WorkManager,
    private val contextScanFilesRepository: ContextScanFilesRepository,
) : CoroutineWorker(context, workerParams) {

    private val userId = UserId(requireNotNull(inputData.getString(WorkerKeys.KEY_USER_ID)))

    override suspend fun doWork(): Result {
        CoreLogger.d(
            BACKUP,
            "BackupFileWatcherWorker triggered by ${triggeredContentUris.size} files"
        )
        contextScanFilesRepository(triggeredContentUris).onSuccess { scanResults ->
            val foundCount =
                scanResults.count { result -> result is ContextScanFilesRepository.ScanResult.Data }
            if(foundCount == 0) {
                CoreLogger.d(BACKUP, "BackupFileWatcherWorker no files were found")
                CoreLogger.d(BACKUP, "BackupFileWatcherWorker will do nothing to prevent empty sync")
            } else {
                CoreLogger.d(BACKUP, "BackupFileWatcherWorker found $foundCount files")
                backupManager.syncAllFolders(userId, UploadFileLink.RECENT_BACKUP_PRIORITY)
            }
            scanResults.forEach { result ->
                if (result is ContextScanFilesRepository.ScanResult.NotFound && result.error != null) {
                    CoreLogger.d(BACKUP, result.error, "Result for file: $result")
                } else {
                    CoreLogger.d(BACKUP, "Result for file: $result")
                }
            }
        }.onFailure { error ->
            error.log(BACKUP, "Cannot read details for files")
        }
        workManager.enqueueUniqueWork(
            uniqueWorkName(userId),
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            getWorkRequest(userId)
        )
        return Result.success()
    }

    companion object {
        fun uniqueWorkName(userId: UserId) = "backup-file-watcher-${userId.id}"

        fun getWorkRequest(userId: UserId) =
            OneTimeWorkRequest.Builder(BackupFileWatcherWorker::class.java)
                .setConstraints(
                    Constraints.Builder()
                        .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
                        .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
                        .build()
                )
                .addTag(userId.id)
                .setInputData(workDataOf(userId))
                .build()

        internal fun workDataOf(userId: UserId) = Data.Builder()
            .putString(WorkerKeys.KEY_USER_ID, userId.id)
            .build()
    }
}
