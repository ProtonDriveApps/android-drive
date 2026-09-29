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
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.photo.data.db.entity.PhotoListingEntity
import me.proton.core.drive.photo.data.db.entity.PhotoListingLinkIds
import me.proton.core.drive.photo.data.db.entity.PhotoListingWithFileProperties
import me.proton.core.drive.sorting.domain.entity.Direction

@Dao
abstract class PhotoListingDao : BaseDao<PhotoListingEntity>() {

    @Query(
        """
            SELECT COUNT(*) FROM (SELECT * FROM PhotoListingEntity WHERE user_id = :userId AND volume_id = :volumeId)
        """
    )
    abstract fun getPhotoListingCount(userId: UserId, volumeId: String): Flow<Int>

    @Query(
        """
            SELECT share_id, id FROM PhotoListingEntity
            WHERE
                user_id = :userId AND
                volume_id = :volumeId AND
                capture_time >= :captureTimeFrom AND
                capture_time < :captureTimeTo
        """
    )
    abstract suspend fun getPhotoListingIds(
        userId: UserId,
        volumeId: String,
        captureTimeFrom: Long,
        captureTimeTo: Long,
    ): List<PhotoListingLinkIds>

    suspend fun getPhotoListingWithFileProperties(
        userId: UserId,
        volumeId: String,
        direction: Direction,
        limit: Int,
        lastCaptureTime: Long?,
        lastId: String?,
    ): List<PhotoListingWithFileProperties> = when(direction) {
        Direction.ASCENDING -> getPhotoListingWithFilePropertiesAsc(userId, volumeId, limit, lastCaptureTime, lastId)
        Direction.DESCENDING -> getPhotoListingWithFilePropertiesDesc(userId, volumeId, limit, lastCaptureTime, lastId)
    }

    @Transaction
    @Query(PHOTO_LISTING_WITH_FILE_PROPERTIES_ASC)
    abstract suspend fun getPhotoListingWithFilePropertiesAsc(
        userId: UserId,
        volumeId: String,
        limit: Int,
        lastCaptureTime: Long?,
        lastId: String?,
    ): List<PhotoListingWithFileProperties>

    @Transaction
    @Query(PHOTO_LISTING_WITH_FILE_PROPERTIES_DESC)
    abstract suspend fun getPhotoListingWithFilePropertiesDesc(
        userId: UserId,
        volumeId: String,
        limit: Int,
        lastCaptureTime: Long?,
        lastId: String?,
    ): List<PhotoListingWithFileProperties>

    fun getPhotoListingWithFilePropertiesFlow(
        userId: UserId,
        volumeId: String,
        direction: Direction,
        limit: Int,
        lastCaptureTime: Long?,
        lastId: String?,
    ): Flow<List<PhotoListingWithFileProperties>> = when (direction) {
        Direction.ASCENDING -> getPhotoListingWithFilePropertiesAscFlow(userId, volumeId, limit, lastCaptureTime, lastId)
        Direction.DESCENDING -> getPhotoListingWithFilePropertiesDescFlow(userId, volumeId, limit, lastCaptureTime, lastId)
    }

    @Transaction
    @Query(PHOTO_LISTING_WITH_FILE_PROPERTIES_ASC)
    abstract fun getPhotoListingWithFilePropertiesAscFlow(
        userId: UserId,
        volumeId: String,
        limit: Int,
        lastCaptureTime: Long?,
        lastId: String?,
    ): Flow<List<PhotoListingWithFileProperties>>

    @Transaction
    @Query(PHOTO_LISTING_WITH_FILE_PROPERTIES_DESC)
    abstract fun getPhotoListingWithFilePropertiesDescFlow(
        userId: UserId,
        volumeId: String,
        limit: Int,
        lastCaptureTime: Long?,
        lastId: String?,
    ): Flow<List<PhotoListingWithFileProperties>>

    @Query("DELETE FROM PhotoListingEntity WHERE user_id = :userId AND share_id = :shareId AND id in (:linkIds)")
    abstract suspend fun delete(userId: UserId, shareId: String, linkIds: List<String>)

    @Query("DELETE FROM PhotoListingEntity WHERE user_id = :userId AND volume_id = :volumeId")
    abstract suspend fun deleteAll(userId: UserId, volumeId: String)

    private companion object{
        const val PHOTO_LISTING_WITH_FILE_PROPERTIES_ASC = """
            SELECT 
                ple.user_id, 
                ple.volume_id, 
                ple.share_id, 
                ple.id, 
                ple.capture_time as capture_time, 
                ple.hash, 
                ple.content_hash, 
                lfpe.revision_id, 
                lfpe.thumbnail_id_default 
            FROM PhotoListingEntity ple
            LEFT JOIN LinkFilePropertiesEntity lfpe ON
                ple.user_id = lfpe.file_user_id AND
                ple.share_id = lfpe.file_share_id AND
                ple.id = lfpe.file_link_id
            WHERE
                ple.user_id = :userId AND
                ple.volume_id = :volumeId AND (
                    :lastCaptureTime IS NULL OR
                    (
                        ple.capture_time > :lastCaptureTime OR
                        (
                            ple.capture_time = :lastCaptureTime AND
                            ple.id > :lastId
                        )
                    )
                )
            ORDER BY ple.capture_time ASC, ple.id ASC
            LIMIT :limit
        """
        const val PHOTO_LISTING_WITH_FILE_PROPERTIES_DESC = """
            SELECT 
                ple.user_id, 
                ple.volume_id, 
                ple.share_id, 
                ple.id, 
                ple.capture_time as capture_time,  
                ple.hash, 
                ple.content_hash, 
                lfpe.revision_id, 
                lfpe.thumbnail_id_default 
            FROM PhotoListingEntity ple
            LEFT JOIN LinkFilePropertiesEntity lfpe ON
                ple.user_id = lfpe.file_user_id AND
                ple.share_id = lfpe.file_share_id AND
                ple.id = lfpe.file_link_id
            WHERE
                ple.user_id = :userId AND
                ple.volume_id = :volumeId AND (
                    :lastCaptureTime IS NULL OR (
                        ple.capture_time < :lastCaptureTime OR
                        (
                            ple.capture_time = :lastCaptureTime AND
                            ple.id < :lastId
                        )
                    )
                )
            ORDER BY ple.capture_time DESC, ple.id DESC
            LIMIT :limit
        """
    }
}
