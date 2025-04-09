/*
 * Copyright (c) 2021-2023 Proton AG.
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

package me.proton.core.drive.drivelink.trash.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.data.db.dao.DriveLinkDao
import me.proton.core.drive.drivelink.data.db.entity.DriveLinkEntity
import me.proton.core.drive.linktrash.data.db.dao.LinkTrashDao

@Dao
interface DriveLinkTrashDao : DriveLinkDao {

    @Transaction
    @Query("""
        SELECT ${DriveLinkDao.DRIVE_LINK_SELECT} FROM ${DriveLinkDao.DRIVE_LINK_ENTITY}
        WHERE
            LinkEntity.user_id = :userId AND
            ShareEntity.volume_id = :volumeId AND
            ${LinkTrashDao.TRASHED_CONDITION}
        LIMIT :limit OFFSET :offset
    """)
    fun getTrashLinks(
        userId: UserId,
        volumeId: String,
        limit: Int,
        offset: Int,
    ): Flow<List<DriveLinkEntity>>

    @Transaction
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT ${DriveLinkDao.DRIVE_LINK_SELECT} FROM ${DriveLinkDao.DRIVE_LINK_ENTITY}
            WHERE
                LinkEntity.user_id = :userId AND
                ShareEntity.volume_id = :volumeId AND
                ${LinkTrashDao.TRASHED_CONDITION}
        )
    """)
    fun getTrashedLinksCount(userId: UserId, volumeId: String): Flow<Int>
}
