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

package me.proton.core.drive.backup.data.manager

import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.extension.uniqueScanWorkName
import me.proton.core.drive.backup.data.worker.BackupCheckDuplicatesWorker
import me.proton.core.drive.backup.data.worker.BackupCleanRevisionsWorker
import me.proton.core.drive.backup.data.worker.BackupFileWatcherWorker
import me.proton.core.drive.backup.data.worker.BackupFindDuplicatesWorker
import me.proton.core.drive.backup.data.worker.BackupNotificationWorker
import me.proton.core.drive.backup.data.worker.BackupScanFolderWorker
import me.proton.core.drive.backup.data.worker.BackupScheduleUploadFolderWorker
import me.proton.core.drive.backup.data.worker.BackupSyncFoldersWorker
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.GetFolders
import me.proton.core.drive.backup.domain.usecase.HasFolders
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.extension.onDisabledOrNotFound
import me.proton.core.drive.feature.flag.domain.extension.onEnabled
import me.proton.core.drive.feature.flag.domain.usecase.WithFeatureFlag
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.time.Duration

class BackupManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val uploadWorkManager: UploadWorkManager,
    private val getFolders: GetFolders,
    private val hasFolders: HasFolders,
    private val withFeatureFlag: WithFeatureFlag,
    private val addBackupError: AddBackupError,
) : BackupManager {

    override suspend fun start(userId: UserId) {
        withFeatureFlag(FeatureFlagId.drivePhotosUploadDisabled(userId)) { featureFlag ->
            featureFlag
                .onDisabledOrNotFound {
                    CoreLogger.d(BACKUP, "start")
                    syncAllFolders(userId)
                }
                .onEnabled {
                    CoreLogger.d(BACKUP, "Backup is disabled by DisableDrivePhotosUpload feature flag")
                    addBackupError(
                        userId = userId,
                        error = BackupError(
                            type = BackupErrorType.PHOTOS_UPLOAD_NOT_ALLOWED,
                            retryable = true,
                        ),
                    )
                }
        }
    }

    override suspend fun stop(userId: UserId) {
        CoreLogger.d(BACKUP, "stop")
        workManager.getWorkInfosByTag(TAG).await()
            .filter { workInfo -> workInfo.tags.contains(userId.id) }
            .forEach { workInfo -> workManager.cancelWorkById(workInfo.id) }
        getFolders(userId).getOrNull()?.forEach { backupFolder ->
            uploadWorkManager.cancelAllByFolder(userId, backupFolder.folderId)
        }
    }

    override fun sync(userId: UserId, backupFolder: BackupFolder, uploadPriority: Long) {
        CoreLogger.d(BACKUP, "Sync bucket: ${backupFolder.bucketId}")
        workManager
            .beginUniqueWork(
                backupFolder.uniqueScanWorkName(userId),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                BackupScanFolderWorker.getWorkRequest(
                    userId = userId,
                    backupFolder = backupFolder,
                    uploadPriority,
                )
            )
            .then(BackupNotificationWorker.getWorkRequest(userId))
            .then(
                BackupFindDuplicatesWorker.getWorkRequest(
                    userId = userId,
                    backupFolder = backupFolder,
                )
            )
            .then(
                BackupCheckDuplicatesWorker.getWorkRequest(
                    userId = userId,
                    backupFolder = backupFolder,
                )
            )
            .then(BackupNotificationWorker.getWorkRequest(userId))
            .then(
                BackupCleanRevisionsWorker.getWorkRequest(
                    userId = userId,
                    folderId = backupFolder.folderId
                )
            )
            .then(
                BackupScheduleUploadFolderWorker.getWorkRequest(
                    userId = userId,
                    backupFolder = backupFolder,
                    delay = Duration.ZERO,
                )
            )
            .enqueue()
    }

    override fun syncAllFolders(userId: UserId, uploadPriority: Long) {
        workManager.enqueue(BackupSyncFoldersWorker.getWorkRequest(userId, uploadPriority))
    }

    override fun watchFolders(userId: UserId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            workManager.enqueueUniqueWork(
                BackupFileWatcherWorker.uniqueWorkName(userId),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                BackupFileWatcherWorker.getWorkRequest(userId),
            )
        }
    }

    override fun unwatchFolders(userId: UserId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            workManager.cancelUniqueWork(BackupFileWatcherWorker.uniqueWorkName(userId))
        }
    }

    override fun isEnabled(userId: UserId): Flow<Boolean> = hasFolders(userId)

    override fun isUploading(): Flow<Boolean> = uploadWorkManager.isUploading(TAG_UPLOAD)


    internal companion object {
        const val TAG = "backup"
        const val TAG_UPLOAD = "backup_upload"
    }
}
