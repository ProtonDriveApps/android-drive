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

package me.proton.core.drive.photo.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.photo.data.db.entity.AlbumListingEntity
import me.proton.core.drive.sorting.domain.entity.Direction

@Dao
abstract class AlbumListingDao : BaseDao<AlbumListingEntity>() {

    @Query(ALBUM_LISTING)
    abstract suspend fun getAlbumListings(
        userId: UserId,
        volumeId: String,
        direction: Direction,
        limit: Int,
        offset: Int,
    ): List<AlbumListingEntity>

    @Query(ALBUM_LISTING)
    abstract fun getAlbumListingsFlow(
        userId: UserId,
        volumeId: String,
        direction: Direction,
        limit: Int,
        offset: Int,
    ): Flow<List<AlbumListingEntity>>

    @Query("DELETE FROM AlbumListingEntity WHERE user_id = :userId AND volume_id = :volumeId")
    abstract suspend fun deleteAll(userId: UserId, volumeId: String)

    companion object {
        const val ALBUM_LISTING = """
            SELECT * FROM AlbumListingEntity
            WHERE
                user_id = :userId AND
                volume_id = :volumeId
            ORDER BY
                CASE WHEN :direction = 'ASCENDING' THEN last_activity_time END ASC,
                CASE WHEN :direction = 'DESCENDING' THEN last_activity_time END DESC
            LIMIT :limit OFFSET :offset
        """
    }
}
