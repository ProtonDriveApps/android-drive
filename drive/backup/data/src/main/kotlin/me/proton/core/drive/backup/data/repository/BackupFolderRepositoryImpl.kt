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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.db.BackupDatabase
import me.proton.core.drive.backup.data.extension.toBackupFolder
import me.proton.core.drive.backup.data.extension.toEntity
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject

class BackupFolderRepositoryImpl @Inject constructor(
    private val db: BackupDatabase,
) : BackupFolderRepository {
    override suspend fun getAll(userId: UserId): List<BackupFolder> =
        db.backupFolderDao.getAll(userId).map { entity -> entity.toBackupFolder() }

    override suspend fun getAll(folderId: FolderId): List<BackupFolder> =
        db.backupFolderDao.getAll(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        ).map { entity -> entity.toBackupFolder() }

    override suspend fun getCount(folderId: FolderId): Int =
        db.backupFolderDao.getCount(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        )
    override fun getAllFlow(folderId: FolderId, count: Int): Flow<List<BackupFolder>> =
        db.backupFolderDao.getAllFlow(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            count = count,
        ).map { entities ->
            entities.map { entity -> entity.toBackupFolder() }
        }

    override suspend fun insertFolder(backupFolder: BackupFolder) : BackupFolder{
        db.backupFolderDao.insertOrIgnore(backupFolder.toEntity())
        return backupFolder
    }

    override suspend fun deleteFolders(folderId: FolderId) {
        db.backupFolderDao.deleteAll(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        )
    }

    override suspend fun deleteFolder(backupFolder: BackupFolder) {
        db.backupFolderDao.delete(backupFolder.toEntity())
    }

    override fun hasFolders(userId: UserId) =
        db.backupFolderDao.hasFolders(userId).distinctUntilChanged()

    override fun hasFolders(folderId: FolderId) =
        db.backupFolderDao.hasFolders(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        ).distinctUntilChanged()

    override suspend fun updateFolder(backupFolder: BackupFolder) {
        db.backupFolderDao.update(backupFolder.toEntity())
    }

    override suspend fun resetAllFoldersUpdateTime(userId: UserId) {
        db.backupFolderDao.resetUpdateTime(userId)
    }

    override suspend fun resetAllFoldersUpdateTime(folderId: FolderId) {
        db.backupFolderDao.resetUpdateTime(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        )
    }

    override suspend fun getFolderByFileUri(userId: UserId, uriString: String): BackupFolder? {
        return db.backupFolderDao.getFolderByFileUri(userId, uriString)?.toBackupFolder()
    }

}
