/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.linktrash.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.linktrash.data.db.entity.TrashMetadataEntity

@Dao
abstract class TrashMetadataDao : BaseDao<TrashMetadataEntity>() {
    @Query(
        QUERY_GET_TRASH_METADATA
    )
    abstract fun getFlow(userId: UserId, shareId: String): Flow<TrashMetadataEntity?>

    @Query(
        QUERY_GET_TRASH_METADATA
    )
    abstract suspend fun get(userId: UserId, shareId: String): TrashMetadataEntity?

    @Query("""
        UPDATE TrashMetadataEntity SET last_fetch_trash_timestamp = :lastFetchTrashTimestamp
            WHERE user_id = :userId AND share_id = :shareId
    """)
    abstract suspend fun updateLastFetchTrashTimestamp(
        userId: UserId,
        shareId: String,
        lastFetchTrashTimestamp: Long?,
    ): Int

    @Transaction
    open suspend fun insertOrUpdate(userId: UserId, shareId: String, lastFetchTrashTimestamp: Long?) {
        if (updateLastFetchTrashTimestamp(userId, shareId, lastFetchTrashTimestamp) == 0) {
            insertOrIgnore(TrashMetadataEntity(userId, shareId, lastFetchTrashTimestamp))
        }
    }

    companion object {
        private const val QUERY_GET_TRASH_METADATA =
            """
                SELECT * FROM TrashMetadataEntity WHERE user_id = :userId AND share_id = :shareId
            """
    }
}
