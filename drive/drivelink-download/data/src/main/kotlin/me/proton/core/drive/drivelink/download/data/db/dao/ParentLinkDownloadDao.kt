/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.drivelink.download.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.download.data.db.entity.ParentLinkDownloadEntity

@Dao
abstract class ParentLinkDownloadDao : BaseDao<ParentLinkDownloadEntity>() {

    @Query("""
        SELECT COUNT(*) FROM ParentLinkDownloadEntity WHERE
            user_id = :userId
    """)
    abstract fun getCountFlow(userId: UserId): Flow<Int>

    @Query("""
        DELETE FROM ParentLinkDownloadEntity WHERE id = :id
    """)
    abstract suspend fun delete(id: Long)

    @Query("""
        DELETE FROM ParentLinkDownloadEntity WHERE
            user_id = :userId AND
            volume_id = :volumeId AND
            share_id = :shareId AND
            link_id = :linkId
    """)
    abstract suspend fun delete(userId: UserId, volumeId: String, shareId: String, linkId: String)

    @Query("""
        DELETE FROM ParentLinkDownloadEntity WHERE user_id = :userId
    """)
    abstract suspend fun deleteAll(userId: UserId)

    @Query("""
        SELECT * FROM ParentLinkDownloadEntity WHERE
            user_id = :userId AND
            type IN (:types)
        ORDER BY id DESC    
        LIMIT :limit OFFSET :offset
    """)
    abstract suspend fun getAll(
        userId: UserId,
        types: Set<Long>,
        limit: Int,
        offset: Int,
    ): List<ParentLinkDownloadEntity>
}
