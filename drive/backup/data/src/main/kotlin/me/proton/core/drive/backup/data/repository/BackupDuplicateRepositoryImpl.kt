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

import me.proton.core.drive.backup.data.db.BackupDatabase
import me.proton.core.drive.backup.data.extension.toBackupDuplicate
import me.proton.core.drive.backup.data.extension.toEntity
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.repository.BackupDuplicateRepository
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject

class BackupDuplicateRepositoryImpl @Inject constructor(
    private val database: BackupDatabase,
) : BackupDuplicateRepository {
    override suspend fun getAll(
        parentId: FolderId,
        fromIndex: Int,
        count: Int,
    ) = database.backupDuplicateDao.getAll(
        parentId.userId,
        parentId.shareId.id,
        parentId.id,
        count,
        fromIndex
    ).map { entity -> entity.toBackupDuplicate() }

    override suspend fun getAllByHash(
        parentId: FolderId,
        hash: String
    ) = database.backupDuplicateDao.getAllByHash(
        parentId.userId,
        parentId.shareId.id,
        parentId.id,
        hash,
    ).map { entity -> entity.toBackupDuplicate() }

    override suspend fun getAllWithState(
        parentId: FolderId,
        state: Link.State,
        fromIndex: Int,
        count: Int,
    ) = database.backupDuplicateDao
        .getAllWithState(
            userId = parentId.userId,
            shareId = parentId.shareId.id,
            parentId = parentId.id,
            state = state,
            limit = count,
            offset = fromIndex,
        )
        .map { entity -> entity.toBackupDuplicate() }

    override suspend fun deleteDuplicates(backupDuplicates: List<BackupDuplicate>) =
        database.backupDuplicateDao.delete(*backupDuplicates
            .map { backupDuplicate -> backupDuplicate.toEntity() }
            .toTypedArray()
        )

    override suspend fun insertDuplicates(duplicates: List<BackupDuplicate>) =
        database.backupDuplicateDao.insertOrUpdate(*duplicates.map { duplicate ->
            duplicate.toEntity()
        }.toTypedArray())
}
