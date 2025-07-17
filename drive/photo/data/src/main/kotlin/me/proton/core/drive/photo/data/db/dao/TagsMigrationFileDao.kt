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
import me.proton.core.drive.photo.data.db.entity.TagsMigrationFileEntity
import me.proton.core.drive.photo.data.db.entity.TagsMigrationStatisticsCount
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile

@Dao
abstract class TagsMigrationFileDao : BaseDao<TagsMigrationFileEntity>() {

    @Query(
        """
        SELECT * FROM TagsMigrationFileEntity
        WHERE user_id = :userId AND
            share_id = :shareId AND
            id = :fileId
        """
    )
    abstract suspend fun getFile(
        userId: UserId,
        shareId: String,
        fileId: String
    ): TagsMigrationFileEntity?

    @Query(
        """
        SELECT * FROM TagsMigrationFileEntity
        WHERE user_id = :userId AND
            volume_id = :volumeId AND
            state = :state
        ORDER BY capture_time DESC
        LIMIT :count
        """
    )
    abstract suspend fun getFilesByState(
        userId: UserId,
        volumeId: String,
        state: TagsMigrationFile.State,
        count: Int,
    ): List<TagsMigrationFileEntity>

    @Query(
        """
        SELECT * FROM TagsMigrationFileEntity
        WHERE user_id = :userId AND
            volume_id = :volumeId AND
            state = :state AND
            (mime_type LIKE "video/%" OR uri IS NOT NULL)
        ORDER BY capture_time DESC
        LIMIT :count
        """
    )
    abstract suspend fun getBatchFilesByState(
        userId: UserId,
        volumeId: String,
        state: TagsMigrationFile.State,
        count: Int,
    ): List<TagsMigrationFileEntity>

    @Suppress("AndroidUnresolvedRoomSqlReference")
    @Query(
        """
        SELECT TagsMigrationFileEntity.* FROM TagsMigrationFileEntity
        JOIN LinkDownloadStateEntity ON 
            LinkDownloadStateEntity.user_id = TagsMigrationFileEntity.user_id AND 
            LinkDownloadStateEntity.share_id = TagsMigrationFileEntity.share_id AND 
            LinkDownloadStateEntity.link_id = TagsMigrationFileEntity.id
        WHERE TagsMigrationFileEntity.user_id = :userId AND
            TagsMigrationFileEntity.volume_id = :volumeId AND
            TagsMigrationFileEntity.state = 'PREPARED' AND
            LinkDownloadStateEntity.state = 'DOWNLOADED'
        ORDER BY TagsMigrationFileEntity.capture_time DESC
        LIMIT 1
        """
    )
    abstract fun getLatestDownloadedFile(
        userId: UserId,
        volumeId: String,
    ): Flow<TagsMigrationFileEntity?>

    @Query(
        """
        SELECT * FROM TagsMigrationFileEntity
        WHERE user_id = :userId AND
            volume_id = :volumeId AND
            state = :state
        ORDER BY capture_time DESC
        LIMIT 1
        """
    )
    abstract fun getLatestFileWithState(
        userId: UserId,
        volumeId: String,
        state: TagsMigrationFile.State,
    ): Flow<TagsMigrationFileEntity?>

    @Query(
        """
        SELECT * FROM TagsMigrationFileEntity
        WHERE user_id = :userId AND
            volume_id = :volumeId AND
            state = :state
        ORDER BY capture_time ASC
        LIMIT 1
        """
    )
    abstract fun getOldestFileWithState(
        userId: UserId,
        volumeId: String,
        state: TagsMigrationFile.State,
    ): Flow<TagsMigrationFileEntity?>

    @Query(
        """
        UPDATE TagsMigrationFileEntity SET state = :state
        WHERE user_id = :userId AND
            share_id = :shareId AND
            id = :fileId
        """
    )
    abstract suspend fun updateState(
        userId: UserId,
        shareId: String,
        fileId: String,
        state: TagsMigrationFile.State
    )

    @Query(
        """
        UPDATE TagsMigrationFileEntity SET uri = :uriString
        WHERE user_id = :userId AND
            share_id = :shareId AND
            id = :fileId
        """
    )
    abstract suspend fun updateUri(
        userId: UserId,
        shareId: String,
        fileId: String,
        uriString: String?
    )

    @Query(
        """
        UPDATE TagsMigrationFileEntity SET mime_type = :mimeType
        WHERE user_id = :userId AND
            share_id = :shareId AND
            id = :fileId
        """
    )
    abstract suspend fun updateMimeType(
        userId: UserId,
        shareId: String,
        fileId: String,
        mimeType: String?
    )

    @Query("DELETE FROM TagsMigrationFileEntity WHERE user_id = :userId AND share_id = :shareId AND id in (:linkIds)")
    abstract suspend fun delete(userId: UserId, shareId: String, linkIds: List<String>)

    @Query("DELETE FROM TagsMigrationFileEntity WHERE user_id = :userId AND volume_id = :volumeId")
    abstract suspend fun deleteAll(userId: UserId, volumeId: String)

    @Query(
        """
        SELECT state, COUNT(*) AS count FROM TagsMigrationFileEntity
        WHERE user_id = :userId AND
            volume_id = :volumeId
        GROUP BY state
        """
    )
    abstract fun getStatistics(
        userId: UserId,
        volumeId: String
    ): Flow<List<TagsMigrationStatisticsCount>>
}
