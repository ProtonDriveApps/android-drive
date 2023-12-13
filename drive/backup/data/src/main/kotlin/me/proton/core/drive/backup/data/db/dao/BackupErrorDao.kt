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
import me.proton.core.drive.backup.data.db.entity.BackupErrorEntity
import me.proton.core.drive.backup.domain.entity.BackupErrorType

@Dao
abstract class BackupErrorDao : BaseDao<BackupErrorEntity>() {
    @Query(
        """
        SELECT * FROM BackupErrorEntity 
        WHERE user_id = :userId 
        LIMIT :limit OFFSET :offset
        """
    )
    abstract fun getAll(
        userId: UserId,
        limit: Int,
        offset: Int,
    ): Flow<List<BackupErrorEntity>>

    @Query("DELETE FROM BackupErrorEntity WHERE user_id = :userId")
    abstract suspend fun deleteAll(userId: UserId)

    @Query("DELETE FROM BackupErrorEntity WHERE user_id = :userId AND error = :type")
    abstract suspend fun deleteAllByType(userId: UserId, type: BackupErrorType)

    @Query("DELETE FROM BackupErrorEntity WHERE user_id = :userId AND retryable = 1")
    abstract suspend fun deleteAllRetryable(userId: UserId)
}
