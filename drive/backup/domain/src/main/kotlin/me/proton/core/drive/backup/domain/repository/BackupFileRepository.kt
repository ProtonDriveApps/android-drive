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

package me.proton.core.drive.backup.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupStateCount
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.link.domain.entity.FolderId

interface BackupFileRepository {
    suspend fun getCountByState(userId: UserId, folderId: FolderId, state: BackupFileState): Int
    suspend fun getAllFiles(userId: UserId, fromIndex: Int, count: Int): List<BackupFile>
    suspend fun getAllInFolderWithState(
        userId: UserId,
        bucketId: Int,
        state: BackupFileState,
        count: Int,
    ): List<BackupFile>

    fun getAllInFolderIdWithState(
        userId: UserId,
        folderId: FolderId,
        state: BackupFileState,
        fromIndex: Int,
        count: Int,
    ): Flow<List<BackupFile>>

    suspend fun getFiles(
        userId: UserId,
        bucketId: Int,
        fromIndex: Int,
        count: Int,
    ): List<BackupFile>

    suspend fun getFilesToBackup(
        userId: UserId,
        bucketId: Int,
        maxAttempts: Long,
        fromIndex: Int,
        count: Int,
    ): List<BackupFile>

    suspend fun insertFiles(userId: UserId, backupFiles: List<BackupFile>)
    suspend fun delete(userId: UserId, uriString: String)
    fun getBackupStatus(userId: UserId): Flow<BackupStatus>
    suspend fun markAsEnqueued(userId: UserId, uriStrings: List<String>)
    suspend fun markAsCompleted(userId: UserId, uriString: String)
    suspend fun markAsFailed(userId: UserId, uriString: String)
    suspend fun markAs(
        userId: UserId,
        uriString: String,
        backupFileState: BackupFileState,
    )
    suspend fun markAs(
        userId: UserId,
        hashes: List<String>,
        backupFileState: BackupFileState,
    )

    suspend fun markAllFailedInFolderAsReady(userId: UserId, bucketId: Int, maxAttempts: Long) : Int
    suspend fun resetFilesAttempts(userId: UserId) : Int
    suspend fun markAllEnqueuedInFolderAsReady(userId: UserId, bucketId: Int): Int
    suspend fun deleteCompletedFromFolder(userId: UserId, bucketId: Int)
    suspend fun deleteFailedForFolderId(userId: UserId, folderId: FolderId)
    suspend fun isBackupCompleteForFolder(userId: UserId, bucketId: Int): Boolean
    suspend fun getStatsForFolder(userId: UserId, bucketId: Int): List<BackupStateCount>
}
