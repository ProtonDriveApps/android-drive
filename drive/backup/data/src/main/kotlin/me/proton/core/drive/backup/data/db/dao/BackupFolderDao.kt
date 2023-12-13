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
import me.proton.core.drive.backup.data.db.entity.BackupFolderEntity

@Dao
abstract class BackupFolderDao : BaseDao<BackupFolderEntity>() {
    @Query("SELECT * FROM BackupFolderEntity WHERE user_id = :userId")
    abstract suspend fun getAll(userId: UserId): List<BackupFolderEntity>

    @Query(
        """
        UPDATE BackupFolderEntity 
        SET update_time = :updateTime
        WHERE user_id = :userId AND bucket_id = :bucketId
        """
    )
    abstract suspend fun updateUpdateTime(
        userId: UserId,
        bucketId: Int,
        updateTime: Long,
    )

    @Query(
        """
        UPDATE BackupFolderEntity 
        SET update_time = NULL
        WHERE user_id = :userId
        """
    )
    abstract suspend fun resetUpdateTime(userId: UserId)

    @Query(
        """
        SELECT BackupFolderEntity.* FROM BackupFolderEntity 
        LEFT JOIN BackupFileEntity ON BackupFolderEntity.bucket_id = BackupFileEntity.bucket_id
        WHERE BackupFolderEntity.user_id = :userId
        AND BackupFileEntity.uri = :uriString
        """
    )
    abstract suspend fun getFolderByFileUri(userId: UserId, uriString: String): BackupFolderEntity?

    @Query("DELETE FROM BackupFolderEntity WHERE user_id = :userId")
    abstract suspend fun deleteAll(userId: UserId)

    @Query("SELECT EXISTS(SELECT * FROM BackupFolderEntity WHERE user_id = :userId)")
    abstract fun hasFolders(userId: UserId): Flow<Boolean>
}
