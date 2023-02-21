/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.volume.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class VolumeDao : BaseDao<VolumeEntity>() {

    @Transaction
    @Query(
        "SELECT EXISTS(SELECT * FROM VolumeEntity WHERE user_id = :userId AND id = :id)"
    )
    abstract suspend fun hasVolumeEntity(userId: UserId, id: String): Boolean

    @Transaction
    @Query(
        "SELECT EXISTS(SELECT * FROM VolumeEntity WHERE user_id = :userId)"
    )
    abstract suspend fun hasVolumeEntities(userId: UserId): Boolean

    @Query(
        QUERY_GET_VOLUME
    )
    abstract fun getFlow(userId: UserId, id: String): Flow<VolumeEntity?>

    @Query(
        QUERY_GET_VOLUME
    )
    abstract suspend fun get(userId: UserId, id: String): VolumeEntity?

    @Query("""
        DELETE FROM VolumeEntity WHERE user_id = :userId AND id = :volumeId
    """)
    abstract suspend fun delete(userId: UserId, volumeId: String)

    @Transaction
    @Query(
        QUERY_GET_ALL_VOLUMES
    )
    abstract fun getAllFlow(userId: UserId): Flow<List<VolumeEntity>>

    @Transaction
    @Query(
        QUERY_GET_ALL_VOLUMES
    )
    abstract suspend fun getAll(userId: UserId): List<VolumeEntity>

    open fun getDistinctFlow(userId: UserId, id: String) = getFlow(userId, id).distinctUntilChanged()

    companion object {
        private const val QUERY_GET_VOLUME =
            """
                SELECT * FROM VolumeEntity WHERE user_id = :userId AND id = :id
            """
        private const val QUERY_GET_ALL_VOLUMES =
            """
                SELECT * FROM VolumeEntity WHERE user_id = :userId
            """
    }
}
