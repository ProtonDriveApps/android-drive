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

package me.proton.core.drive.photo.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.photo.data.db.entity.PhotoListingEntity
import me.proton.core.drive.sorting.domain.entity.Direction

@Dao
abstract class PhotoListingDao : BaseDao<PhotoListingEntity>() {

    @Query(
        """
            SELECT COUNT(*) FROM (SELECT * FROM PhotoListingEntity WHERE user_id = :userId AND volume_id = :volumeId)
        """
    )
    abstract fun getPhotoListingCount(userId: UserId, volumeId: String): Flow<Int>

    suspend fun getPhotoListings(
        userId: UserId,
        volumeId: String,
        direction: Direction,
        limit: Int,
        offset: Int,
    ): List<PhotoListingEntity> = when(direction) {
        Direction.ASCENDING -> getPhotoListingsAsc(userId, volumeId, limit, offset)
        Direction.DESCENDING -> getPhotoListingsDesc(userId, volumeId, limit, offset)
    }

    @Query(PHOTO_LISTING_ASC)
    abstract suspend fun getPhotoListingsAsc(
        userId: UserId,
        volumeId: String,
        limit: Int,
        offset: Int,
    ): List<PhotoListingEntity>

    @Query(PHOTO_LISTING_DESC)
    abstract suspend fun getPhotoListingsDesc(
        userId: UserId,
        volumeId: String,
        limit: Int,
        offset: Int,
    ): List<PhotoListingEntity>

    fun getPhotoListingsFlow(
        userId: UserId,
        volumeId: String,
        direction: Direction,
        limit: Int,
        offset: Int,
    ): Flow<List<PhotoListingEntity>> = when (direction) {
        Direction.ASCENDING -> getPhotoListingsAscFlow(userId, volumeId, limit, offset)
        Direction.DESCENDING -> getPhotoListingsDescFlow(userId, volumeId, limit, offset)
    }

    @Query(PHOTO_LISTING_ASC)
    abstract fun getPhotoListingsAscFlow(
        userId: UserId,
        volumeId: String,
        limit: Int,
        offset: Int,
    ): Flow<List<PhotoListingEntity>>

    @Query(PHOTO_LISTING_DESC)
    abstract fun getPhotoListingsDescFlow(
        userId: UserId,
        volumeId: String,
        limit: Int,
        offset: Int,
    ): Flow<List<PhotoListingEntity>>

    @Query("DELETE FROM PhotoListingEntity WHERE user_id = :userId AND share_id = :shareId AND id in (:linkIds)")
    abstract suspend fun delete(userId: UserId, shareId: String, linkIds: List<String>)

    @Query("DELETE FROM PhotoListingEntity WHERE user_id = :userId AND volume_id = :volumeId")
    abstract suspend fun deleteAll(userId: UserId, volumeId: String)

    private companion object{
        const val PHOTO_LISTING_ASC = """
            SELECT * FROM PhotoListingEntity
            WHERE
                user_id = :userId AND
                volume_id = :volumeId
            ORDER BY capture_time ASC, id ASC
            LIMIT :limit OFFSET :offset
        """
        const val PHOTO_LISTING_DESC = """
            SELECT * FROM PhotoListingEntity
            WHERE
                user_id = :userId AND
                volume_id = :volumeId
            ORDER BY capture_time DESC, id DESC
            LIMIT :limit OFFSET :offset
        """
    }
}
