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

package me.proton.core.drive.backup.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.db.entity.BackupDuplicateEntity
import me.proton.core.drive.link.domain.entity.Link

@Dao
abstract class BackupDuplicateDao : BaseDao<BackupDuplicateEntity>() {
    @Query(
        """
        SELECT * FROM `BackupDuplicateEntity`
        WHERE user_id = :userId AND 
            share_id = :shareId AND
            parent_id = :parentId
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getAll(
        userId: UserId,
        shareId: String,
        parentId: String,
        limit: Int,
        offset: Int,
    ): List<BackupDuplicateEntity>

    @Query(
        """
        SELECT * FROM `BackupDuplicateEntity`
        WHERE user_id = :userId AND 
            share_id = :shareId AND
            parent_id = :parentId AND
            hash = :hash
        """
    )
    abstract suspend fun getAllByHash(
        userId: UserId,
        shareId: String,
        parentId: String,
        hash: String,
    ): List<BackupDuplicateEntity>

    @Query(
        """
        SELECT * FROM `BackupDuplicateEntity`
        WHERE user_id = :userId AND 
            share_id = :shareId AND
            parent_id = :parentId AND
            state = :state
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getAllWithState(
        userId: UserId,
        shareId: String,
        parentId: String,
        state: Link.State,
        limit: Int,
        offset: Int,
    ): List<BackupDuplicateEntity>
}
