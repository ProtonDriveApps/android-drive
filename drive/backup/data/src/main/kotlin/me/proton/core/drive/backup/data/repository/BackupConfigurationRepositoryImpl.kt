/*
 * Copyright (c) 2024 Proton AG.
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
import me.proton.core.drive.backup.data.extension.toBackupConfiguration
import me.proton.core.drive.backup.data.extension.toEntity
import me.proton.core.drive.backup.domain.entity.BackupConfiguration
import me.proton.core.drive.backup.domain.repository.BackupConfigurationRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject

class BackupConfigurationRepositoryImpl @Inject constructor(
    private val database: BackupDatabase,
) : BackupConfigurationRepository {
    override suspend fun insertConfiguration(configuration: BackupConfiguration) {
        database.backupConfigurationDao.insertOrUpdate(configuration.toEntity())
    }

    override fun get(folderId: FolderId): Flow<BackupConfiguration?> =
        database.backupConfigurationDao.get(
            userId = folderId.userId,
            shareId = folderId.shareId.id,
            folderId = folderId.id,
        ).map { entity ->
            entity?.toBackupConfiguration()
        }

    override fun getAll(userId: UserId): Flow<List<BackupConfiguration>> =
        database.backupConfigurationDao.getAll(
            userId = userId,
        ).map { entities ->
            entities.map { entity -> entity.toBackupConfiguration() }
        }
}
