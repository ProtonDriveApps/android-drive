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

package me.proton.core.drive.backup.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.drive.backup.data.db.BackupDatabase
import me.proton.core.drive.backup.data.extension.toBackupFile
import me.proton.core.drive.backup.data.extension.toBackupStateCount
import me.proton.core.drive.backup.data.extension.toEntity
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFileState.COMPLETED
import me.proton.core.drive.backup.domain.entity.BackupFileState.DUPLICATED
import me.proton.core.drive.backup.domain.entity.BackupFileState.ENQUEUED
import me.proton.core.drive.backup.domain.entity.BackupFileState.FAILED
import me.proton.core.drive.backup.domain.entity.BackupFileState.IDLE
import me.proton.core.drive.backup.domain.entity.BackupFileState.POSSIBLE_DUPLICATE
import me.proton.core.drive.backup.domain.entity.BackupFileState.READY
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupStateCount
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.data.db.DatabaseLimits.MAX_VARIABLE_NUMBER
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject

class BackupFileRepositoryImpl @Inject constructor(
    private val db: BackupDatabase,
) : BackupFileRepository {
    override suspend fun getCountByState(
        folderId: FolderId,
        state: BackupFileState,
    ): Int = db.backupFileDao.getCountFlowByState(
        userId = folderId.userId,
        shareId = folderId.shareId.id,
        folderId = folderId.id,
        state = state
    )

    override suspend fun getAllFiles(folderId: FolderId, fromIndex: Int, count: Int): List<BackupFile> =
        db.backupFileDao.getAll(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            limit = count,
            offset = fromIndex
        ).map { entity ->
            entity.toBackupFile()
        }

    override suspend fun getAllInFolderWithState(
        folderId: FolderId,
        bucketId: Int,
        state: BackupFileState,
        count: Int,
    ): List<BackupFile> =
        db.backupFileDao.getAllInFolderWithState(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            bucketId = bucketId,
            state = state,
            limit = count,
        ).map { entity ->
            entity.toBackupFile()
        }

    override fun getAllInFolderIdWithState(
        folderId: FolderId,
        state: BackupFileState,
        fromIndex: Int,
        count: Int,
    ): Flow<List<BackupFile>> =
        db.backupFileDao.getAllInFolderIdWithState(
            userId = folderId.userId,
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
        folderId: FolderId,
        bucketId: Int,
        fromIndex: Int,
        count: Int,
    ): List<BackupFile> =
        db.backupFileDao.getAllInFolder(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            bucketId = bucketId,
            limit = count,
            offset = fromIndex
        ).map { entity ->
            entity.toBackupFile()
        }

    override suspend fun getFilesToBackup(
        folderId: FolderId,
        bucketId: Int,
        maxAttempts: Long,
        fromIndex: Int,
        count: Int,
    ): List<BackupFile> =
        db.backupFileDao.getAllInFolderToBackup(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            bucketId = bucketId,
            maxAttempts = maxAttempts,
            limit = count,
            offset = fromIndex
        ).map { entity ->
            entity.toBackupFile()
        }

    override suspend fun insertFiles(backupFiles: List<BackupFile>) {
        backupFiles.map { media -> media.toEntity() }.let { entities ->
            db.backupFileDao.insertOrIgnore(*entities.toTypedArray())
        }
    }

    override suspend fun delete(folderId: FolderId, uriString: String) {
        db.backupFileDao.delete(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            uriString = uriString,
        )
    }

    override fun getBackupStatus(folderId: FolderId): Flow<BackupStatus> {
        return db.backupFileDao.getProgression(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        ).distinctUntilChanged().map { progress ->
            val preparing = progress.filter { it.backupFileState in listOf(IDLE, POSSIBLE_DUPLICATE) }.sumOf { it.count }
            val pending = progress.filter { it.backupFileState in listOf(READY, ENQUEUED) }.sumOf { it.count }
            val failed = progress.filter { it.backupFileState in listOf(FAILED) }.sumOf { it.count }
            val total = progress.filterNot { it.backupFileState in listOf(DUPLICATED) }.sumOf { it.count }
            when {
                preparing != 0 -> BackupStatus.Preparing(total, preparing)
                pending == 0 -> if (failed == 0) {
                    BackupStatus.Complete(total)
                } else {
                    BackupStatus.Uncompleted(total, failed)
                }
                else -> BackupStatus.InProgress(total, pending)
            }
        }
    }

    override suspend fun markAsEnqueued(folderId: FolderId, uriStrings: List<String>) {
        db.inTransaction {
            uriStrings.chunked(MAX_VARIABLE_NUMBER).forEach { chunk ->
                db.backupFileDao.updateState(
                    userId = folderId.userId,
                    shareId = folderId.shareId.id,
                    folderId = folderId.id,
                    uriStrings = chunk,
                    state = ENQUEUED,
                )
            }
        }
    }

    override suspend fun markAsCompleted(folderId: FolderId, uriString: String) {
        db.backupFileDao.updateState(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            uri = uriString,
            state = COMPLETED,
        )
    }

    override suspend fun markAs(
        folderId: FolderId,
        uriString: String,
        backupFileState: BackupFileState,
    ) {
        db.backupFileDao.updateState(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            uri = uriString,
            state = backupFileState,
        )
    }

    override suspend fun markAsFailed(folderId: FolderId, uriString: String) {
        db.inTransaction {
            db.backupFileDao.updateState(
                userId = folderId.userId,
                shareId = folderId.shareId.id,
                folderId = folderId.id,
                uri = uriString,
                state = FAILED,
            )
            db.backupFileDao.incrementAttempts(
                userId = folderId.userId,
                shareId = folderId.shareId.id,
                folderId = folderId.id,
                uriString,
            )
        }
    }

    override suspend fun markAs(
        folderId: FolderId,
        bucketId: Int,
        hashes: List<String>,
        backupFileState: BackupFileState,
    ) {
        db.inTransaction {
            hashes.chunked(MAX_VARIABLE_NUMBER).forEach { chunk ->
                db.backupFileDao.updateStateByHash(
                    userId = folderId.userId,
                    shareId = folderId.shareId.id,
                    folderId = folderId.id,
                    bucketId = bucketId,
                    hashes = chunk,
                    state = backupFileState,
                )
            }
        }
    }

    override suspend fun markAllFilesInFolderIdAsIdle(folderId: FolderId) : Int =
        db.backupFileDao.updateStateInFolder(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            target = IDLE,
        )

    override suspend fun markAllFailedInFolderAsReady(
        folderId: FolderId,
        bucketId: Int,
        maxAttempts: Long,
    ): Int =
        db.backupFileDao.updateStateInFolder(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            bucketId = bucketId,
            source = FAILED,
            target = READY,
            maxAttempts = maxAttempts,
        )

    override suspend fun resetFilesAttempts(folderId: FolderId): Int =
        db.backupFileDao.resetFilesAttempts(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        )

    override suspend fun markAllEnqueuedInFolderAsReady(backupFolder: BackupFolder): Int =
        db.backupFileDao.updateStateInFolder(
            userId = backupFolder.folderId.userId,
            shareId = backupFolder.folderId.shareId.id,
            folderId = backupFolder.folderId.id,
            bucketId = backupFolder.bucketId,
            source = ENQUEUED,
            target = READY,
        )

    override suspend fun deleteCompletedFromFolder(backupFolder: BackupFolder) {
        db.backupFileDao.deleteFromFolderWithState(
            backupFolder.folderId.userId,
            backupFolder.folderId.shareId.id,
            backupFolder.folderId.id,
            backupFolder.bucketId,
            DUPLICATED,
            COMPLETED
        )
    }

    override suspend fun deleteFailedForFolderId(folderId: FolderId) {
        db.backupFileDao.deleteForFolderIdWithState(
            folderId.userId,
            folderId.shareId.id,
            folderId.id,
            FAILED,
        )
    }

    override suspend fun isBackupCompleteForFolder(backupFolder: BackupFolder): Boolean {
        return db.backupFileDao.isAllFilesInState(
            backupFolder.folderId.userId,
            backupFolder.folderId.shareId.id,
            backupFolder.folderId.id,
            backupFolder.bucketId,
            DUPLICATED,
            COMPLETED
        )
    }

    override suspend fun getStatsForFolder(
        backupFolder: BackupFolder,
    ): List<BackupStateCount> {
        return db.backupFileDao.getStatsForFolder(
            backupFolder.folderId.userId,
            backupFolder.folderId.shareId.id,
            backupFolder.folderId.id,
            backupFolder.bucketId,
        ).map { entity -> entity.toBackupStateCount() }
    }

}
