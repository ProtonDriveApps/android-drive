/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.log.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.log.data.db.entity.LogEntity

@Dao
abstract class LogDao : BaseDao<LogEntity>() {

    @Query("""
        SELECT * FROM LogEntity WHERE
            user_id = :userId AND
            level NOT IN (SELECT level FROM LogLevelEntity WHERE user_id = :userId) AND
            origin NOT IN (SELECT origin FROM LogOriginEntity WHERE user_id = :userId)
        ORDER BY creation_time DESC
    """)
    abstract fun getLogsPagingSource(
        userId: UserId,
    ): PagingSource<Int, LogEntity>

    @Query("""
        SELECT * FROM LogEntity WHERE user_id = :userId ORDER BY creation_time DESC LIMIT :limit OFFSET :offset
    """)
    abstract suspend fun getLogs(userId: UserId, limit: Int, offset: Int): List<LogEntity>

    @Query("""
        DELETE FROM LogEntity WHERE
            user_id = :userId AND
            id NOT IN (SELECT id FROM LogEntity WHERE user_id = :userId ORDER BY creation_time DESC LIMIT :limit)
    """)
    abstract suspend fun dropOldRowsToFitLimit(userId: UserId, limit: Int)

    @Query("SELECT COUNT(*) FROM LogEntity WHERE user_id = :userId")
    abstract suspend fun countByUser(userId: UserId): Int

    @Query("""
        DELETE FROM LogEntity WHERE
            id IN (SELECT id FROM LogEntity WHERE user_id = :userId ORDER BY creation_time ASC LIMIT 1)
    """)
    abstract suspend fun deleteOldest(userId: UserId)


    @Transaction
    open suspend fun insertWithLimit(userId: UserId, entity: LogEntity, limit: Int) {
        if (countByUser(userId) >= limit) {
            deleteOldest(userId)
        }
        insertOrIgnore(entity)
    }

    @Query("""
        DELETE FROM LogEntity WHERE user_id = :userId
    """)
    abstract suspend fun deleteAll(userId: UserId)
}
