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
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.FolderId

interface BackupFolderRepository {
    suspend fun getAll(userId: UserId): List<BackupFolder>
    suspend fun getCount(folderId: FolderId): Int
    fun getAllFlow(folderId: FolderId, count: Int): Flow<List<BackupFolder>>

    suspend fun insertFolder(backupFolder: BackupFolder) : BackupFolder
    suspend fun deleteFolders(userId: UserId)
    suspend fun deleteFolder(userId: UserId, bucketId: Int)
    suspend fun updateFolderUpdateTime(userId: UserId, bucketId: Int, updateTime: TimestampS)
    suspend fun resetAllFoldersUpdateTime(userId: UserId)
    suspend fun getFolderByFileUri(userId: UserId, uriString: String): BackupFolder?
    fun hasFolders(userId: UserId): Flow<Boolean>
}
