/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.android.drive.photos.data.db.entity.AddToAlbumEntity
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class AddToAlbumDao : BaseDao<AddToAlbumEntity>() {

    @Query("""
        SELECT COUNT(*) FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_id IS NULL
    """)
    abstract fun getPhotoListingsCount(userId: UserId): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_share_id = :albumShareId AND
            album_id = :albumId
    """)
    abstract fun getPhotoListingsCount(userId: UserId, albumShareId: String, albumId: String): Flow<Int>

    @Query("""
        SELECT * FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_id IS NULL
        ORDER BY capture_time DESC
    """)
    abstract fun getAddToAlbumPhotosPagingSource(
        userId: UserId,
    ): PagingSource<Int, AddToAlbumEntity>

    @Query("""
        SELECT * FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_share_id = :albumShareId AND
            album_id = :albumId
        ORDER BY capture_time DESC
    """)
    abstract fun getAddToAlbumPhotosPagingSource(
        userId: UserId,
        albumShareId: String,
        albumId: String,
    ): PagingSource<Int, AddToAlbumEntity>

    @Query("""
        SELECT * FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_id IS NULL
        ORDER BY capture_time DESC
        LIMIT :limit OFFSET :offset
    """)
    abstract suspend fun getPhotoListings(
        userId: UserId,
        limit: Int,
        offset: Int,
    ): List<AddToAlbumEntity>

    @Query("""
        SELECT * FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_share_id = :albumShareId AND
            album_id = :albumId
        ORDER BY capture_time DESC
        LIMIT :limit OFFSET :offset
    """)
    abstract suspend fun getPhotoListings(
        userId: UserId,
        albumShareId: String,
        albumId: String,
        limit: Int,
        offset: Int,
    ): List<AddToAlbumEntity>

    @Query("""
        DELETE FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_id IS NULL AND
            share_id = :shareId AND
            link_id IN (:linkIds)
    """)
    abstract suspend fun delete(userId: UserId, shareId: String, linkIds: Set<String>)

    @Query("""
        DELETE FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_share_id = :albumShareId AND
            album_id = :albumId AND
            share_id = :shareId AND
            link_id IN (:linkIds)
    """)
    abstract suspend fun delete(
        userId: UserId,
        albumShareId: String,
        albumId: String,
        shareId: String,
        linkIds: Set<String>,
    )

    @Query("""
        DELETE FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_id IS NULL
    """)
    abstract suspend fun deleteAll(userId: UserId)

    @Query("""
        DELETE FROM AddToAlbumEntity WHERE
            user_id = :userId AND
            album_share_id = :albumShareId AND
            album_id = :albumId
    """)
    abstract suspend fun deleteAll(userId: UserId, albumShareId: String, albumId: String)
}
