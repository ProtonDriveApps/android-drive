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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.db.BackupDatabase
import me.proton.core.drive.backup.data.extension.toBackupError
import me.proton.core.drive.backup.data.extension.toEntity
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.backup.domain.repository.BackupErrorRepository
import javax.inject.Inject

class BackupErrorRepositoryImpl @Inject constructor(
    private val db: BackupDatabase,
) : BackupErrorRepository {
    override fun getAll(userId: UserId, fromIndex: Int, count: Int): Flow<List<BackupError>> =
        db.backupErrorDao.getAll(userId, count, fromIndex).map { backupErrorEntities ->
            backupErrorEntities.map { backupErrorEntity ->
                backupErrorEntity.toBackupError()
            }
        }

    override suspend fun deleteAll(userId: UserId) {
        db.backupErrorDao.deleteAll(userId)
    }

    override suspend fun deleteAllByType(userId: UserId, type: BackupErrorType) {
        db.backupErrorDao.deleteAllByType(userId, type)
    }

    override suspend fun deleteAllRetryable(userId: UserId) {
        db.backupErrorDao.deleteAllRetryable(userId)
    }

    override suspend fun insertError(userId: UserId, error: BackupError) {
        db.backupErrorDao.insertOrUpdate(error.toEntity(userId))
    }
}
