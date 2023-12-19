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

package me.proton.core.drive.backup.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.db.BackupDatabase
import me.proton.core.drive.backup.data.extension.toBackupFile
import me.proton.core.drive.backup.data.extension.toBackupStateCount
import me.proton.core.drive.backup.data.extension.toEntity
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupStateCount
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.data.db.DatabaseLimits.MAX_VARIABLE_NUMBER
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

class BackupFileRepositoryImpl @Inject constructor(
    private val db: BackupDatabase,
) : BackupFileRepository {
    override suspend fun getCountByState(
        userId: UserId,
        folderId: FolderId,
        state: BackupFileState,
    ): Int = db.backupFileDao.getCountFlowByState(
        userId = userId,
        shareId = folderId.shareId.id,
        folderId = folderId.id,
        state = state
    )

    override suspend fun getAllFiles(userId: UserId, fromIndex: Int, count: Int): List<BackupFile> =
        db.backupFileDao.getAll(
            userId = userId,
            limit = count,
            offset = fromIndex
        ).map { entity ->
            entity.toBackupFile()
        }

    override suspend fun getAllInFolderWithState(
        userId: UserId,
        bucketId: Int,
        state: BackupFileState,
        count: Int,
    ): List<BackupFile> =
        db.backupFileDao.getAllInFolderWithState(
            userId = userId,
            bucketId = bucketId,
            state = state,
            limit = count,
        ).map { entity ->
            entity.toBackupFile()
        }

    override fun getAllInFolderIdWithState(
        userId: UserId,
        folderId: FolderId,
        state: BackupFileState,
        fromIndex: Int,
        count: Int,
    ): Flow<List<BackupFile>> =
        db.backupFileDao.getAllInFolderIdWithState(
            userId = userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            state = state,
            limit = count,
            offset = fromIndex
        ).map { entities ->
            entities.map { entity ->
                entity.toBackupFile()
            }
        }

    override suspend fun getFiles(
        userId: UserId,
        bucketId: Int,
        fromIndex: Int,
        count: Int,
    ): List<BackupFile> =
        db.backupFileDao.getAllInFolder(
            userId = userId,
            bucketId = bucketId,
            limit = count,
            offset = fromIndex
        ).map { entity ->
            entity.toBackupFile()
        }

    override suspend fun getFilesToBackup(
        userId: UserId,
        bucketId: Int,
        maxAttempts: Long,
        fromIndex: Int,
        count: Int,
    ): List<BackupFile> =
        db.backupFileDao.getAllInFolderToBackup(
            userId = userId,
            bucketId = bucketId,
            maxAttempts = maxAttempts,
            limit = count,
            offset = fromIndex
        ).map { entity ->
            entity.toBackupFile()
        }

    override suspend fun insertFiles(userId: UserId, backupFiles: List<BackupFile>) {
        backupFiles.map { media -> media.toEntity(userId) }.let { entities ->
            db.backupFileDao.insertOrIgnore(*entities.toTypedArray())
        }
    }

    override suspend fun delete(userId: UserId, uriString: String) {
        db.backupFileDao.delete(userId, uriString)
    }

    override fun getBackupStatus(userId: UserId): Flow<BackupStatus> {
        return db.backupFileDao.getProgression(userId).distinctUntilChanged().map { progress ->
            val (pending, failed, total) = progress
            if (pending == 0) {
                if (failed == 0) {
                    BackupStatus.Complete(total)
                } else {
                    BackupStatus.Uncompleted(total, failed)
                }
            } else {
                BackupStatus.InProgress(total, pending)
            }
        }
    }

    override suspend fun markAsEnqueued(userId: UserId, uriStrings: List<String>) {
        db.inTransaction {
            uriStrings.chunked(MAX_VARIABLE_NUMBER).forEach { chunk ->
                db.backupFileDao.updateState(userId, chunk, BackupFileState.ENQUEUED)
            }
        }
    }

    override suspend fun markAsCompleted(userId: UserId, uriString: String) {
        db.backupFileDao.updateState(userId, uriString, BackupFileState.COMPLETED)
    }

    override suspend fun markAs(
        userId: UserId,
        uriString: String,
        backupFileState: BackupFileState,
    ) {
        db.backupFileDao.updateState(userId, uriString, backupFileState)
    }

    override suspend fun markAsFailed(userId: UserId, uriString: String) {
        db.inTransaction {
            db.backupFileDao.updateState(userId, uriString, BackupFileState.FAILED)
            db.backupFileDao.incrementAttempts(userId, uriString)
        }
    }

    override suspend fun markAs(
        userId: UserId,
        hashes: List<String>,
        backupFileState: BackupFileState,
    ) {
        db.inTransaction {
            hashes.chunked(MAX_VARIABLE_NUMBER).forEach { chunk ->
                db.backupFileDao.updateStateByHash(userId, chunk, backupFileState)
            }
        }
    }

    override suspend fun markAllFailedInFolderAsReady(
        userId: UserId,
        bucketId: Int,
        maxAttempts: Long,
    ): Int =
        db.backupFileDao.updateStateInFolder(
            userId = userId,
            bucketId = bucketId,
            source = BackupFileState.FAILED,
            target = BackupFileState.READY,
            maxAttempts = maxAttempts,
        )

    override suspend fun resetFilesAttempts(userId: UserId): Int =
        db.backupFileDao.resetFilesAttempts(userId)

    override suspend fun markAllEnqueuedInFolderAsReady(userId: UserId, bucketId: Int) : Int =
        db.backupFileDao.updateStateInFolder(
            userId = userId,
            bucketId = bucketId,
            source = BackupFileState.ENQUEUED,
            target = BackupFileState.READY,
        )

    override suspend fun deleteCompletedFromFolder(userId: UserId, bucketId: Int) {
        db.backupFileDao.deleteFromFolderWithState(
            userId,
            bucketId,
            BackupFileState.DUPLICATED,
            BackupFileState.COMPLETED
        )
    }

    override suspend fun deleteFailedForFolderId(userId: UserId, folderId: FolderId) {
        db.backupFileDao.deleteForFolderIdWithState(
            userId,
            folderId.shareId.id,
            folderId.id,
            BackupFileState.FAILED,
        )
    }

    override suspend fun isBackupCompleteForFolder(userId: UserId, bucketId: Int): Boolean {
        return db.backupFileDao.isAllFilesInState(
            userId,
            bucketId,
            BackupFileState.DUPLICATED,
            BackupFileState.COMPLETED
        )
    }

    override suspend fun getStatsForFolder(
        userId: UserId,
        bucketId: Int,
    ): List<BackupStateCount> {
        return db.backupFileDao.getStatsForFolder(userId, bucketId)
            .map { entity -> entity.toBackupStateCount() }
    }

}
