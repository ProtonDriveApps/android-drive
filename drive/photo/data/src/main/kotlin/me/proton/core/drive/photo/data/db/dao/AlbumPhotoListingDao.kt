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
import me.proton.core.drive.photo.data.db.entity.AlbumPhotoListingEntity
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.sorting.domain.entity.Direction

@Dao
abstract class AlbumPhotoListingDao : BaseDao<AlbumPhotoListingEntity>() {

    @Query(
        """
            SELECT COUNT(*) FROM (
                SELECT * FROM AlbumPhotoListingEntity
                WHERE
                    user_id = :userId AND
                    volume_id = :volumeId AND
                    album_id = :albumId
            )
        """
    )
    abstract fun getAlbumPhotoListingCount(
        userId: UserId,
        volumeId: String,
        albumId: String,
    ): Flow<Int>

    @Query(ALBUM_PHOTO_LISTING)
    abstract suspend fun getAlbumPhotoListings(
        userId: UserId,
        volumeId: String,
        albumId: String,
        sortingBy: PhotoListing.Album.SortBy,
        direction: Direction,
        limit: Int,
        offset: Int,
    ): List<AlbumPhotoListingEntity>

    @Query(ALBUM_PHOTO_LISTING)
    abstract fun getAlbumPhotoListingsFlow(
        userId: UserId,
        volumeId: String,
        albumId: String,
        sortingBy: PhotoListing.Album.SortBy,
        direction: Direction,
        limit: Int,
        offset: Int,
    ): Flow<List<AlbumPhotoListingEntity>>

    @Query("DELETE FROM AlbumPhotoListingEntity WHERE user_id = :userId AND volume_id = :volumeId AND album_id = :albumId")
    abstract suspend fun deleteAll(userId: UserId, volumeId: String, albumId: String)

    companion object {
        const val ALBUM_PHOTO_LISTING = """
            SELECT * FROM AlbumPhotoListingEntity
            WHERE
                user_id = :userId AND
                volume_id = :volumeId AND
                album_id = :albumId
            ORDER BY
                CASE WHEN :direction = 'ASCENDING' AND :sortingBy = 'CAPTURED' THEN capture_time END ASC,
                CASE WHEN :direction = 'ASCENDING' AND :sortingBy = 'ADDED' THEN added_time END ASC,
                CASE WHEN :direction = 'DESCENDING' AND :sortingBy = 'CAPTURED' THEN capture_time END DESC,
                CASE WHEN :direction = 'DESCENDING' AND :sortingBy = 'ADDED' THEN added_time END DESC
            LIMIT :limit OFFSET :offset
        """
    }
}
