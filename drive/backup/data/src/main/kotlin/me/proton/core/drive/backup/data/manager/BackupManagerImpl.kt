/*
 * Copyright (c) 2023-2024 Proton AG.
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

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.await
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.extension.uniqueScanWorkName
import me.proton.core.drive.backup.data.worker.BackupCheckDuplicatesWorker
import me.proton.core.drive.backup.data.worker.BackupCleanRevisionsWorker
import me.proton.core.drive.backup.data.worker.BackupEnabledWorker
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
import me.proton.core.drive.backup.domain.usecase.GetAllFolders
import me.proton.core.drive.backup.domain.usecase.HasFolders
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.extension.onDisabledOrNotFound
import me.proton.core.drive.feature.flag.domain.extension.onEnabled
import me.proton.core.drive.feature.flag.domain.usecase.WithFeatureFlag
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.time.Duration

class BackupManagerImpl @Inject constructor(
    private val workManager: WorkManager,
    private val uploadWorkManager: UploadWorkManager,
    private val getAllFolders: GetAllFolders,
    private val hasFolders: HasFolders,
    private val withFeatureFlag: WithFeatureFlag,
    private val addBackupError: AddBackupError,
) : BackupManager {

    override suspend fun start(folderId: FolderId) {
        withFeatureFlag(FeatureFlagId.drivePhotosUploadDisabled(folderId.userId)) { featureFlag ->
            featureFlag
                .onDisabledOrNotFound {
                    CoreLogger.d(BACKUP, "start")
                    startForegroundWork(folderId.userId)
                    syncAllFolders(folderId)
                }
                .onEnabled {
                    CoreLogger.i(BACKUP, "Backup is disabled by DrivePhotosUploadDisabled feature flag")
                    addBackupError(
                        folderId = folderId,
                        error = BackupError(
                            type = BackupErrorType.PHOTOS_UPLOAD_NOT_ALLOWED,
                            retryable = true,
                        ),
                    )
                }
        }
    }

    override suspend fun stop(folderId: FolderId) {
        CoreLogger.i(BACKUP, "Stopping backup for ${folderId.id.logId()}")
        getAllFolders(folderId).getOrNull()?.forEach { backupFolder ->
            uploadWorkManager.cancelAllByFolder(backupFolder.folderId.userId, backupFolder.folderId)
        }
        workManager.getWorkInfosByTag(TAG).await()
            .filter { workInfo -> workInfo.tags.contains(folderId.id) }
            .forEach { workInfo -> workManager.cancelWorkById(workInfo.id) }
    }

    override suspend fun cancelSync(backupFolder: BackupFolder) {
        CoreLogger.i(BACKUP, "Canceling sync: ${backupFolder.bucketId}")
        workManager.cancelUniqueWork(
            backupFolder.uniqueScanWorkName()
        ).await()
    }

    override fun sync(backupFolder: BackupFolder, uploadPriority: Long) {
        CoreLogger.i(BACKUP, "Sync bucket: ${backupFolder.bucketId}")
        workManager
            .beginUniqueWork(
                backupFolder.uniqueScanWorkName(),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                BackupScanFolderWorker.getWorkRequest(
                    backupFolder = backupFolder,
                    uploadPriority = uploadPriority,
                )
            )
            .then(BackupNotificationWorker.getWorkRequest(backupFolder.folderId))
            .then(
                BackupFindDuplicatesWorker.getWorkRequest(
                    backupFolder = backupFolder,
                )
            )
            .then(
                BackupCheckDuplicatesWorker.getWorkRequest(
                    backupFolder = backupFolder,
                )
            )
            .then(BackupNotificationWorker.getWorkRequest(backupFolder.folderId))
            .then(
                BackupCleanRevisionsWorker.getWorkRequest(
                    folderId = backupFolder.folderId
                )
            )
            .then(
                BackupScheduleUploadFolderWorker.getWorkRequest(
                    backupFolder = backupFolder,
                    delay = Duration.ZERO,
                )
            )
            .enqueue()
    }

    override fun syncAllFolders(folderId: FolderId, uploadPriority: Long) {
        workManager.enqueue(BackupSyncFoldersWorker.getWorkRequest(folderId, uploadPriority))
    }

    override suspend fun watchFolders(userId: UserId) {
        CoreLogger.d(BACKUP, "Watch folders for $userId")
        workManager.enqueueUniqueWork(
            BackupFileWatcherWorker.uniqueWorkName(userId),
            ExistingWorkPolicy.KEEP,
            BackupFileWatcherWorker.getWorkRequest(userId),
        ).await()
    }

    override suspend fun unwatchFolders(userId: UserId) {
        CoreLogger.d(BACKUP, "Unwatch folders for $userId")
        workManager.cancelUniqueWork(BackupFileWatcherWorker.uniqueWorkName(userId)).await()
    }

    override fun isEnabled(folderId: FolderId): Flow<Boolean> = hasFolders(folderId)

    override suspend fun updateNotification(folderId: FolderId) {
        workManager.enqueue(
            BackupNotificationWorker.getWorkRequest(folderId)
        ).await()
    }

    override suspend fun cancelForegroundWork(userId: UserId) {
        workManager.cancelUniqueWork(BackupEnabledWorker.getUniqueWorkName(userId))
    }

    private suspend fun startForegroundWork(userId: UserId) {
        workManager.enqueueUniqueWork(
            BackupEnabledWorker.getUniqueWorkName(userId),
            ExistingWorkPolicy.KEEP,
            BackupEnabledWorker.getWorkRequest(userId),
        ).await()
    }

    internal companion object {
        const val TAG = "backup"
    }
}
