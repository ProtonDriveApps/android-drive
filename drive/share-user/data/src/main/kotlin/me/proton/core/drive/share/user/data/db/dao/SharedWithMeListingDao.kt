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

package me.proton.core.drive.share.user.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.user.data.db.entity.SharedWithMeListingEntity

@Dao
abstract class SharedWithMeListingDao : BaseDao<SharedWithMeListingEntity>() {

    @Query(
        """
            SELECT * FROM SharedWithMeListingEntity
            WHERE
                user_id = :userId AND
                type IN (:types) OR (:includeNullType AND type IS NULL)
            LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun getSharedWithMeListing(
        userId: UserId,
        types: Set<Long>,
        includeNullType: Boolean,
        limit: Int,
        offset: Int,
    ): List<SharedWithMeListingEntity>

    @Query(
        """
            SELECT COUNT(*) FROM SharedWithMeListingEntity
            WHERE
                user_id = :userId AND
                type IN (:types) OR (:includeNullType AND type IS NULL)
        """
    )
    abstract suspend fun getSharedWithMeListingCount(
        userId: UserId,
        types: Set<Long>,
        includeNullType: Boolean,
    ): Int

    @Query(
        """
            SELECT * FROM SharedWithMeListingEntity
            WHERE
                user_id = :userId AND
                type IN (:types) OR (:includeNullType AND type IS NULL)
        """
    )
    abstract fun getSharedWithMeListingPagingSource(
        userId: UserId,
        types: Set<Long>,
        includeNullType: Boolean,
    ): PagingSource<Int, SharedWithMeListingEntity>

    @Query(
        """
            DELETE FROM SharedWithMeListingEntity
            WHERE
                user_id = :userId AND
                volume_id = :volumeId AND
                share_id = :shareId AND
                link_id in (:linkIds)
        """
    )
    abstract fun delete(userId: UserId, volumeId: String, shareId: String, linkIds: List<String>)

    @Query("DELETE FROM SharedWithMeListingEntity WHERE user_id = :userId")
    abstract fun deleteAll(userId: UserId)
}
