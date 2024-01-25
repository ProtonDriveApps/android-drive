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
import kotlinx.coroutines.flow.map
import me.proton.core.drive.backup.data.db.BackupDatabase
import me.proton.core.drive.backup.data.extension.toBackupError
import me.proton.core.drive.backup.data.extension.toEntity
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.backup.domain.repository.BackupErrorRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject

class BackupErrorRepositoryImpl @Inject constructor(
    private val db: BackupDatabase,
) : BackupErrorRepository {
    override fun getAll(folderId: FolderId, fromIndex: Int, count: Int): Flow<List<BackupError>> =
        db.backupErrorDao.getAll(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            limit = count,
            offset = fromIndex,
        ).map { backupErrorEntities ->
            backupErrorEntities.map { backupErrorEntity ->
                backupErrorEntity.toBackupError()
            }
        }

    override suspend fun deleteAll(folderId: FolderId) {
        db.backupErrorDao.deleteAll(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        )
    }

    override suspend fun deleteAllByType(folderId: FolderId, type: BackupErrorType) {
        db.backupErrorDao.deleteAllByType(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
            type = type,
        )
    }

    override suspend fun deleteAllRetryable(folderId: FolderId) {
        db.backupErrorDao.deleteAllRetryable(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        )
    }

    override suspend fun insertError(folderId: FolderId, error: BackupError) {
        db.backupErrorDao.insertOrUpdate(error.toEntity(folderId))
    }
}
