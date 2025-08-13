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

package me.proton.core.drive.drivelink.download.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.download.data.db.entity.FileDownloadEntity
import me.proton.core.drive.drivelink.download.domain.entity.DownloadFileLink

@Dao
abstract class FileDownloadDao : BaseDao<FileDownloadEntity>() {

    @Query("""
        SELECT * FROM FileDownloadEntity WHERE
            user_id = :userId AND
            state = :state
        ORDER BY priority, id ASC
    """)
    abstract suspend fun getNext(userId: UserId, state: DownloadFileLink.State): FileDownloadEntity?

    @Query("""
        SELECT * FROM FileDownloadEntity WHERE
            user_id = :userId AND
            volume_id = :volumeId AND
            share_id = :shareId AND
            link_id = :fileId
    """)
    abstract suspend fun get(
        userId: UserId,
        volumeId: String,
        shareId: String,
        fileId: String,
    ): FileDownloadEntity?

    @Query("""
        SELECT * FROM FileDownloadEntity WHERE id = :id
    """)
    abstract suspend fun get(
        id: Long,
    ): FileDownloadEntity?

    @Query("""
        SELECT * FROM FileDownloadEntity WHERE
            user_id = :userId AND
            state = :state
        ORDER BY priority, id ASC
        LIMIT :count OFFSET :offset
    """)
    abstract suspend fun getAll(
        userId: UserId,
        state: DownloadFileLink.State,
        offset: Int,
        count: Int,
    ): List<FileDownloadEntity>

    @Query("""
        DELETE FROM FileDownloadEntity WHERE id = :id
    """)
    abstract suspend fun delete(id: Long)

    @Query("""
        DELETE FROM FileDownloadEntity WHERE
            user_id = :userId AND
            volume_id = :volumeId AND
            share_id = :shareId AND
            link_id = :fileId AND
            revision_id = :revisionId
    """)
    abstract suspend fun delete(
        userId: UserId,
        volumeId: String,
        shareId: String,
        fileId: String,
        revisionId: String,
    )

    @Query("""
        DELETE FROM FileDownloadEntity WHERE user_id = :userId
    """)
    abstract suspend fun deleteAll(userId: UserId)

    @Query("""
        UPDATE FileDownloadEntity SET state = :state WHERE user_id = :userId
    """)
    abstract suspend fun resetAllState(userId: UserId, state: DownloadFileLink.State)

    @Query("""
        SELECT COUNT(*) FROM FileDownloadEntity WHERE user_id = :userId
    """)
    abstract suspend fun getCount(userId: UserId): Int

    @Query("""
        SELECT COUNT(*) FROM FileDownloadEntity WHERE
            user_id = :userId AND
            volume_id = :volumeId AND
            parent_id = :linkId
    """)
    abstract suspend fun getCount(userId: UserId, volumeId: String, linkId: String): Int

    @Query("""
        SELECT COUNT(*) FROM FileDownloadEntity WHERE user_id = :userId
    """)
    abstract fun getCountFlow(userId: UserId): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM FileDownloadEntity WHERE user_id = :userId AND state = :state
    """)
    abstract fun getCountFlow(userId: UserId, state: DownloadFileLink.State): Flow<Int>
}
