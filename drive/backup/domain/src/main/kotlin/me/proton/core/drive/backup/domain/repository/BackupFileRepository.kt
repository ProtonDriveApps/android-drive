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

package me.proton.core.drive.backup.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupStateCount
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.link.domain.entity.FolderId

interface BackupFileRepository {
    suspend fun getCountByState(folderId: FolderId, state: BackupFileState): Int
    suspend fun getFile(folderId: FolderId, uriString: String): BackupFile?
    suspend fun getAllFiles(folderId: FolderId, fromIndex: Int, count: Int): List<BackupFile>
    suspend fun getAllInFolderWithState(
        folderId: FolderId,
        bucketId: Int,
        state: BackupFileState,
        count: Int,
    ): List<BackupFile>

    fun getAllInFolderIdWithState(
        folderId: FolderId,
        state: BackupFileState,
        fromIndex: Int,
        count: Int,
    ): Flow<List<BackupFile>>

    suspend fun getFiles(
        folderId: FolderId,
        bucketId: Int,
        fromIndex: Int,
        count: Int,
    ): List<BackupFile>

    suspend fun getFilesToBackup(
        folderId: FolderId,
        bucketId: Int,
        maxAttempts: Long,
        fromIndex: Int,
        count: Int,
    ): List<BackupFile>

    suspend fun insertFiles(backupFiles: List<BackupFile>)
    suspend fun delete(folderId: FolderId, uriString: String)
    fun getBackupStatus(folderId: FolderId): Flow<BackupStatus>
    suspend fun markAsEnqueued(folderId: FolderId, uriStrings: List<String>)
    suspend fun markAsCompleted(folderId: FolderId, uriString: String)
    suspend fun markAsFailed(folderId: FolderId, uriString: String)
    suspend fun markAs(
        folderId: FolderId,
        uriString: String,
        backupFileState: BackupFileState,
    )
    suspend fun markAs(
        folderId: FolderId,
        bucketId: Int,
        hashes: List<String>,
        backupFileState: BackupFileState,
    )

    suspend fun markAllFilesInFolderIdAsIdle(folderId: FolderId) : Int
    suspend fun markAllFailedInFolderAsReady(folderId: FolderId, bucketId: Int, maxAttempts: Long) : Int
    suspend fun resetFilesAttempts(folderId: FolderId) : Int
    suspend fun markAllEnqueuedInFolderAsReady(backupFolder: BackupFolder): Int
    suspend fun deleteCompletedFromFolder(backupFolder: BackupFolder)
    suspend fun deleteFailedForFolderId(folderId: FolderId)
    suspend fun isBackupCompleteForFolder(backupFolder: BackupFolder): Boolean
    suspend fun getStatsForFolder(backupFolder: BackupFolder): List<BackupStateCount>
}
