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

package me.proton.core.drive.stats.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.stats.data.db.entity.UploadStatsEntity

@Dao
abstract class UploadStatsDao : BaseDao<UploadStatsEntity>() {
    @Query(
        """
        SELECT * FROM UploadStatsEntity
        WHERE user_id = :userId
        AND share_id = :shareId
        AND link_id = :folderId
        """
    )
    abstract suspend fun get(
        userId: UserId,
        shareId: String,
        folderId: String,
    ): UploadStatsEntity?

    @Query(
        """
        DELETE FROM UploadStatsEntity
        WHERE user_id = :userId
        AND share_id = :shareId
        AND link_id = :folderId
        """
    )
    abstract suspend fun delete(
        userId: UserId,
        shareId: String,
        folderId: String,
    )

    @Transaction
    open suspend fun aggregate(entity: UploadStatsEntity): UploadStatsEntity {
        val updatedEntity = get(
            userId = entity.userId,
            shareId = entity.shareId,
            folderId = entity.folderId
        )?.let { folderProgressEntity ->
            folderProgressEntity.copy(
                count = folderProgressEntity.count + entity.count,
                size = folderProgressEntity.size + entity.size,
                minimumUploadCreationDateTime = minOf(
                    entity.minimumUploadCreationDateTime,
                    folderProgressEntity.minimumUploadCreationDateTime
                ),
                minimumFileCreationDateTime = minOfNullables(
                    entity.minimumFileCreationDateTime,
                    folderProgressEntity.minimumFileCreationDateTime
                ),
            )
        } ?: entity
        insertOrUpdate(updatedEntity)
        return updatedEntity
    }

    private fun minOfNullables(
        a: Long?,
        b: Long?,
    ) = when {
        a == null -> b
        b == null -> a
        else -> minOf(a, b)
    }

}
