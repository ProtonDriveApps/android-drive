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

package me.proton.core.drive.worker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class WorkerDao : BaseDao<WorkerRunEntity>() {
    @Query("""
        SELECT * FROM WorkerRunEntity WHERE user_id = :userId AND worker_id = :workerId
    """)
    abstract suspend fun getAll(userId: UserId, workerId: String): List<WorkerRunEntity>

    @Insert
    abstract suspend fun insert(entity: WorkerRunEntity): Long

    @Query("""
        UPDATE WorkerRunEntity SET blocked = :blocked WHERE user_id = :userId AND worker_id = :workerId AND id = :id
    """)
    abstract suspend fun markBlocked(userId: UserId, workerId: String, id: Long, blocked: Int)

    @Query("""
        DELETE FROM WorkerRunEntity WHERE user_id = :userId AND worker_id = :workerId
    """)
    abstract suspend fun deleteAll(userId: UserId, workerId: String)
}
