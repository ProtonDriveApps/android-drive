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
package me.proton.core.drive.folder.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class FolderMetadataDao : BaseDao<FolderMetadataEntity>() {
    @Query(
        QUERY_GET_FOLDER_METADATA
    )
    abstract fun getFlow(userId: UserId, shareId: String, linkId: String): Flow<FolderMetadataEntity?>

    @Query(
        QUERY_GET_FOLDER_METADATA
    )
    abstract suspend fun get(userId: UserId, shareId: String, linkId: String): FolderMetadataEntity?

    @Query("""
        UPDATE FolderMetadataEntity SET last_fetch_children_timestamp = :lastFetchChildrenTimestamp
            WHERE user_id = :userId AND share_id = :shareId AND link_id = :linkId
    """)
    abstract suspend fun updateLastFetchChildrenTimestamp(
        userId: UserId,
        shareId: String,
        linkId: String,
        lastFetchChildrenTimestamp: Long?,
    ): Int

    @Transaction
    open suspend fun insertOrUpdate(
        userId: UserId,
        shareId: String,
        linkId: String,
        lastFetchChildrenTimestamp: Long?,
    ) {
        if (updateLastFetchChildrenTimestamp(userId, shareId, linkId, lastFetchChildrenTimestamp) == 0) {
            insertOrIgnore(FolderMetadataEntity(userId, shareId, linkId, lastFetchChildrenTimestamp))
        }
    }

    companion object {
        private const val QUERY_GET_FOLDER_METADATA =
            """
                SELECT * FROM FolderMetadataEntity WHERE user_id = :userId AND share_id = :shareId AND link_id = :linkId
            """
    }
}
