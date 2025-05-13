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

    suspend fun getAlbumPhotoListings(
        userId: UserId,
        volumeId: String,
        albumId: String,
        sortingBy: PhotoListing.Album.SortBy,
        direction: Direction,
        limit: Int,
        offset: Int,
    ): List<AlbumPhotoListingEntity> = when(direction){
        Direction.ASCENDING -> getAlbumPhotoListingsAsc(
            userId = userId,
            volumeId = volumeId,
            albumId = albumId,
            sortingBy = sortingBy,
            limit = limit,
            offset = offset,
        )
        Direction.DESCENDING -> getAlbumPhotoListingsDesc(
            userId = userId,
            volumeId = volumeId,
            albumId = albumId,
            sortingBy = sortingBy,
            limit = limit,
            offset = offset,
        )
    }

    fun getAlbumPhotoListingsFlow(
        userId: UserId,
        volumeId: String,
        albumId: String,
        sortingBy: PhotoListing.Album.SortBy,
        direction: Direction,
        limit: Int,
        offset: Int,
    ): Flow<List<AlbumPhotoListingEntity>> = when (direction) {
        Direction.ASCENDING -> getAlbumPhotoListingsAscFlow(
            userId = userId,
            volumeId = volumeId,
            albumId = albumId,
            sortingBy = sortingBy,
            limit = limit,
            offset = offset,
        )

        Direction.DESCENDING -> getAlbumPhotoListingsDescFlow(
            userId = userId,
            volumeId = volumeId,
            albumId = albumId,
            sortingBy = sortingBy,
            limit = limit,
            offset = offset,
        )
    }

    @Query(ALBUM_PHOTO_LISTING_ASC)
    internal abstract suspend fun getAlbumPhotoListingsAsc(
        userId: UserId,
        volumeId: String,
        albumId: String,
        sortingBy: PhotoListing.Album.SortBy,
        limit: Int,
        offset: Int,
    ): List<AlbumPhotoListingEntity>

    @Query(ALBUM_PHOTO_LISTING_ASC)
    internal abstract fun getAlbumPhotoListingsAscFlow(
        userId: UserId,
        volumeId: String,
        albumId: String,
        sortingBy: PhotoListing.Album.SortBy,
        limit: Int,
        offset: Int,
    ): Flow<List<AlbumPhotoListingEntity>>

    @Query(ALBUM_PHOTO_LISTING_DESC)
    internal abstract suspend fun getAlbumPhotoListingsDesc(
        userId: UserId,
        volumeId: String,
        albumId: String,
        sortingBy: PhotoListing.Album.SortBy,
        limit: Int,
        offset: Int,
    ): List<AlbumPhotoListingEntity>

    @Query(ALBUM_PHOTO_LISTING_DESC)
    internal abstract fun getAlbumPhotoListingsDescFlow(
        userId: UserId,
        volumeId: String,
        albumId: String,
        sortingBy: PhotoListing.Album.SortBy,
        limit: Int,
        offset: Int,
    ): Flow<List<AlbumPhotoListingEntity>>

    @Query("""
        DELETE FROM AlbumPhotoListingEntity WHERE
            user_id = :userId AND
            volume_id = :volumeId AND
            album_id = :albumId AND
            id IN (:linkIds)
    """)
    abstract suspend fun delete(userId: UserId, volumeId: String, albumId: String, linkIds: List<String>)

    @Query("DELETE FROM AlbumPhotoListingEntity WHERE user_id = :userId AND volume_id = :volumeId AND album_id = :albumId")
    abstract suspend fun deleteAll(userId: UserId, volumeId: String, albumId: String)

    @Query("DELETE FROM AlbumPhotoListingEntity WHERE user_id = :userId AND share_id = :shareId AND id in (:linkIds)")
    abstract suspend fun delete(userId: UserId, shareId: String, linkIds: List<String>)

    companion object {
        const val ALBUM_PHOTO_LISTING_ASC = """
            SELECT * FROM AlbumPhotoListingEntity
            WHERE
                user_id = :userId AND
                volume_id = :volumeId AND
                album_id = :albumId
            ORDER BY
                CASE WHEN :sortingBy = 'ADDED' THEN added_time END ASC,
                capture_time ASC,
                id ASC
            LIMIT :limit OFFSET :offset
        """
        const val ALBUM_PHOTO_LISTING_DESC = """
            SELECT * FROM AlbumPhotoListingEntity
            WHERE
                user_id = :userId AND
                volume_id = :volumeId AND
                album_id = :albumId
            ORDER BY
                CASE WHEN :sortingBy = 'ADDED' THEN added_time END DESC,
                capture_time DESC,
                id DESC
            LIMIT :limit OFFSET :offset
        """
    }
}
