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

package me.proton.core.drive.backup.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.db.entity.BackupFileEntity
import me.proton.core.drive.backup.data.db.entity.BackupProgression
import me.proton.core.drive.backup.data.db.entity.BackupStateCountEntity
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.base.data.db.Column

@Dao
abstract class BackupFileDao : BaseDao<BackupFileEntity>() {
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT BackupFileEntity.* FROM BackupFileEntity
            JOIN BackupFolderEntity ON BackupFolderEntity.bucket_id = BackupFileEntity.bucket_id
            WHERE BackupFolderEntity.user_id = :userId AND
            BackupFolderEntity.share_id = :shareId AND
            BackupFolderEntity.parent_id = :folderId AND
            BackupFileEntity.state = :state 
        )"""
    )
    abstract suspend fun getCountFlowByState(
        userId: UserId,
        shareId: String,
        folderId: String,
        state: BackupFileState,
    ): Int

    @Query("SELECT * FROM BackupFileEntity WHERE user_id = :userId ORDER BY ${Column.CREATION_TIME} DESC LIMIT :limit OFFSET :offset")
    abstract suspend fun getAll(userId: UserId, limit: Int, offset: Int): List<BackupFileEntity>

    @Query(
        """
        SELECT * FROM BackupFileEntity
        WHERE user_id = :userId AND bucket_id = :bucketId AND state = :state
        ORDER BY ${Column.CREATION_TIME} DESC LIMIT :limit
        """
    )
    abstract suspend fun getAllInFolderWithState(
        userId: UserId,
        bucketId: Int,
        state: BackupFileState,
        limit: Int,
    ): List<BackupFileEntity>

    @Query(
        """
        SELECT BackupFileEntity.* FROM BackupFileEntity
        JOIN BackupFolderEntity ON BackupFileEntity.bucket_id = BackupFolderEntity.bucket_id
        WHERE BackupFolderEntity.user_id = :userId AND 
        BackupFolderEntity.share_id = :shareId AND 
        BackupFolderEntity.parent_id = :folderId AND 
        BackupFileEntity.state = :state
        ORDER BY ${Column.CREATION_TIME} DESC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract fun getAllInFolderIdWithState(
        userId: UserId,
        shareId: String,
        folderId: String,
        state: BackupFileState,
        limit: Int,
        offset: Int,
    ): Flow<List<BackupFileEntity>>

    @Query("SELECT * FROM BackupFileEntity WHERE user_id = :userId AND bucket_id = :bucketId ORDER BY ${Column.CREATION_TIME} DESC LIMIT :limit OFFSET :offset")
    abstract suspend fun getAllInFolder(
        userId: UserId,
        bucketId: Int,
        limit: Int,
        offset: Int,
    ): List<BackupFileEntity>

    @Query(
        """
        SELECT 
            BackupFileEntity.*
        FROM 
            BackupFileEntity
        LEFT JOIN LinkUploadEntity ON
            LinkUploadEntity.uri = BackupFileEntity.uri
        WHERE 
            BackupFileEntity.user_id = :userId AND  
            BackupFileEntity.bucket_id = :bucketId AND
            LinkUploadEntity.uri IS NULL AND
            BackupFileEntity.state == "READY" AND
            BackupFileEntity.attempts < :maxAttempts
        ORDER BY ${Column.CREATION_TIME} DESC
        LIMIT :limit
        OFFSET :offset"""
    )
    abstract suspend fun getAllInFolderToBackup(
        userId: UserId,
        bucketId: Int,
        maxAttempts: Long,
        limit: Int,
        offset: Int,
    ): List<BackupFileEntity>

    @Query("DELETE FROM BackupFileEntity WHERE user_id = :userId AND uri = :uriString")
    abstract suspend fun delete(userId: UserId, uriString: String)


    @Query(
        """
        SELECT (
            SELECT COUNT(*) FROM BackupFileEntity 
            WHERE 
                BackupFileEntity.user_id = :userId AND 
                BackupFileEntity.state IN ("IDLE", "POSSIBLE_DUPLICATE", "READY", "ENQUEUED")
        ) as pending,(
            SELECT COUNT(*) FROM BackupFileEntity 
            WHERE 
                BackupFileEntity.user_id = :userId AND 
                BackupFileEntity.state IN ("FAILED")
        ) as failed,(
            SELECT COUNT(*) FROM BackupFileEntity 
            WHERE 
                BackupFileEntity.user_id = :userId AND 
                BackupFileEntity.state IN("IDLE", "POSSIBLE_DUPLICATE", "READY", "ENQUEUED", "FAILED", "COMPLETED") 
        ) as total
        """
    )
    abstract fun getProgression(userId: UserId): Flow<BackupProgression>

    @Query(
        """
        UPDATE BackupFileEntity SET state = :state
        WHERE user_id = :userId AND uri = :uri
        """
    )
    abstract suspend fun updateState(userId: UserId, uri: String, state: BackupFileState)

    @Query(
        """
        UPDATE BackupFileEntity SET state = :state
        WHERE user_id = :userId AND uri IN (:uriStrings)
        """
    )
    abstract suspend fun updateState(userId: UserId, uriStrings: List<String>, state: BackupFileState)


    @Query(
        """
        UPDATE BackupFileEntity SET attempts = attempts + 1
        WHERE user_id = :userId AND uri = :uri
        """
    )
    abstract suspend fun incrementAttempts(userId: UserId, uri: String)

    @Query(
        """
        UPDATE BackupFileEntity SET state = :state
        WHERE user_id = :userId AND hash IN (:hashes)
        """
    )
    abstract suspend fun updateStateByHash(userId: UserId, hashes: List<String>, state: BackupFileState)

    @Query(
        """
        UPDATE BackupFileEntity SET state = :target
        WHERE user_id = :userId AND 
        bucket_id = :bucketId AND 
        state = :source
        """
    )
    abstract suspend fun updateStateInFolder(
        userId: UserId,
        bucketId: Int,
        source: BackupFileState,
        target: BackupFileState,
    ): Int
    @Query(
        """
        UPDATE BackupFileEntity SET state = :target
        WHERE user_id = :userId AND 
        bucket_id = :bucketId AND 
        state = :source AND
        attempts < :maxAttempts
        """
    )
    abstract suspend fun updateStateInFolder(
        userId: UserId,
        bucketId: Int,
        source: BackupFileState,
        target: BackupFileState,
        maxAttempts: Long,
    ): Int
    @Query(
        """
        UPDATE BackupFileEntity SET attempts = 0
        WHERE user_id = :userId 
        """
    )
    abstract suspend fun resetFilesAttempts(userId: UserId) : Int
    @Query(
        """
        DELETE FROM BackupFileEntity
        WHERE user_id = :userId AND 
            bucket_id = :bucketId AND 
            state in (:state)
        """
    )
    abstract suspend fun deleteFromFolderWithState(
        userId: UserId,
        bucketId: Int,
        vararg state: BackupFileState,
    )
    @Query(
        """
        DELETE FROM BackupFileEntity
        WHERE user_id = :userId AND 
            bucket_id IN (
                SELECT BackupFolderEntity.bucket_id FROM BackupFolderEntity
                WHERE BackupFolderEntity.user_id = :userId AND 
                BackupFolderEntity.share_id = :shareId AND 
                BackupFolderEntity.parent_id = :folderId
            ) AND 
            state in (:state)
        """
    )
    abstract suspend fun deleteForFolderIdWithState(
        userId: UserId,
        shareId: String,
        folderId: String,
        vararg state: BackupFileState,
    )

    @Query(
        """
        SELECT NOT EXISTS (SELECT * FROM BackupFileEntity 
            WHERE user_id = :userId AND 
                bucket_id = :bucketId AND 
                state not in (:state)   )
        
        """
    )
    abstract suspend fun isAllFilesInState(
        userId: UserId,
        bucketId: Int,
        vararg state: BackupFileState,
    ): Boolean

    @Query(
        """
        SELECT 
            BackupFileEntity.state AS backupFileState, 
            LinkUploadEntity.state AS uploadState, 
            COUNT(*) AS count
        FROM 
            BackupFileEntity
        LEFT JOIN LinkUploadEntity ON
            LinkUploadEntity.uri = BackupFileEntity.uri
        WHERE 
            BackupFileEntity.user_id = :userId AND  
            BackupFileEntity.bucket_id = :bucketId
        GROUP BY backupFileState, uploadState
        ORDER BY backupFileState, uploadState ASC
        """
    )
    abstract suspend fun getStatsForFolder(userId: UserId, bucketId: Int) : List<BackupStateCountEntity>
}
