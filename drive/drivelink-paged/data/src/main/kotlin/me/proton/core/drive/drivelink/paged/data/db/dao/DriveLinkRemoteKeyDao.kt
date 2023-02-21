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

package me.proton.core.drive.drivelink.paged.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.paged.data.db.entity.DriveLinkRemoteKeyEntity
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId

@Dao
abstract class DriveLinkRemoteKeyDao : BaseDao<DriveLinkRemoteKeyEntity>() {

    @Query(
        """
        SELECT * FROM DriveLinkRemoteKeyEntity
        WHERE 
            user_id = :userId AND 
            `key` = :key
        ORDER BY previous_key DESC
        LIMIT 1
    """
    )
    abstract suspend fun getLastRemoteKey(userId: UserId, key: String): DriveLinkRemoteKeyEntity?

    @Query(
        """
        SELECT * FROM DriveLinkRemoteKeyEntity
        WHERE 
            user_id = :userId AND 
            `key` = :key AND 
            share_id = :shareId AND 
            link_id = :linkId
    """
    )
    abstract suspend fun getLinkRemoteKey(
        userId: UserId,
        key: String,
        shareId: String,
        linkId: String,
    ): DriveLinkRemoteKeyEntity?

    @Query(
        """
            DELETE FROM DriveLinkRemoteKeyEntity WHERE user_id = :userId AND `key` = :key
        """
    )
    abstract suspend fun deleteKeys(userId: UserId, key: String)

    suspend fun getLinkRemoteKey(key: String, linkId: LinkId) =
        getLinkRemoteKey(linkId.userId, key, linkId.shareId.id, linkId.id)
}
