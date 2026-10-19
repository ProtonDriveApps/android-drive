/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.recently.accessed.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.recently.accessed.data.db.entity.RecentlyAccessedEntity
import me.proton.core.drive.recently.accessed.data.db.entity.RecentlyAccessedWithLinkType

@Dao
abstract class RecentlyAccessedDao : BaseDao<RecentlyAccessedEntity>() {

    @Query("""
        INSERT OR IGNORE INTO RecentlyAccessedEntity (user_id, share_id, link_id, volume_id, access_time)
        VALUES (:userId, :shareId, :linkId, :volumeId, :accessTime)
    """)
    protected abstract suspend fun insertOrIgnore(
        userId: UserId,
        shareId: String,
        linkId: String,
        volumeId: String,
        accessTime: Long,
    )

    @Query("""
        UPDATE RecentlyAccessedEntity SET volume_id = :volumeId, access_time = MAX(access_time, :accessTime)
        WHERE user_id = :userId AND share_id = :shareId AND link_id = :linkId
    """)
    protected abstract suspend fun update(
        userId: UserId,
        shareId: String,
        linkId: String,
        volumeId: String,
        accessTime: Long,
    )

    @Transaction
    open suspend fun upsert(userId: UserId, shareId: String, linkId: String, volumeId: String, accessTime: Long) {
        insertOrIgnore(userId, shareId, linkId, volumeId, accessTime)
        update(userId, shareId, linkId, volumeId, accessTime)
    }

    @Query("""
        SELECT RecentlyAccessedEntity.*,
            CASE
                WHEN LinkFilePropertiesEntity.file_link_id IS NOT NULL THEN ${LinkDto.TYPE_FILE}
                WHEN LinkFolderPropertiesEntity.folder_link_id IS NOT NULL THEN ${LinkDto.TYPE_FOLDER}
                ELSE ${LinkDto.TYPE_ALBUM}
            END AS link_type
        FROM RecentlyAccessedEntity
        LEFT JOIN LinkFilePropertiesEntity ON
            RecentlyAccessedEntity.user_id = LinkFilePropertiesEntity.file_user_id AND
            RecentlyAccessedEntity.share_id = LinkFilePropertiesEntity.file_share_id AND
            RecentlyAccessedEntity.link_id = LinkFilePropertiesEntity.file_link_id
        LEFT JOIN LinkFolderPropertiesEntity ON
            RecentlyAccessedEntity.user_id = LinkFolderPropertiesEntity.folder_user_id AND
            RecentlyAccessedEntity.share_id = LinkFolderPropertiesEntity.folder_share_id AND
            RecentlyAccessedEntity.link_id = LinkFolderPropertiesEntity.folder_link_id
        LEFT JOIN LinkAlbumPropertiesEntity ON
            RecentlyAccessedEntity.user_id = LinkAlbumPropertiesEntity.album_user_id AND
            RecentlyAccessedEntity.share_id = LinkAlbumPropertiesEntity.album_share_id AND
            RecentlyAccessedEntity.link_id = LinkAlbumPropertiesEntity.album_link_id
        WHERE RecentlyAccessedEntity.user_id = :userId AND (
            LinkFilePropertiesEntity.file_link_id IS NOT NULL OR
            LinkFolderPropertiesEntity.folder_link_id IS NOT NULL OR
            LinkAlbumPropertiesEntity.album_link_id IS NOT NULL
        )
        ORDER BY RecentlyAccessedEntity.access_time DESC
        LIMIT :limit
    """)
    abstract suspend fun getAll(userId: UserId, limit: Int): List<RecentlyAccessedWithLinkType>

    @Query("""
        DELETE FROM RecentlyAccessedEntity
        WHERE user_id = :userId AND share_id = :shareId AND link_id IN (:linkIds) AND access_time <= :accessTime
    """)
    abstract suspend fun delete(userId: UserId, shareId: String, linkIds: List<String>, accessTime: Long)
}
