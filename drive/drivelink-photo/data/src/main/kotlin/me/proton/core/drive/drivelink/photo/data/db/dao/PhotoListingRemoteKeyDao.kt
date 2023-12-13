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

package me.proton.core.drive.drivelink.photo.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.drivelink.photo.data.db.entity.PhotoListingRemoteKeyEntity
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId

@Dao
abstract class PhotoListingRemoteKeyDao : BaseDao<PhotoListingRemoteKeyEntity>() {

    @Query(
        """
        SELECT * FROM PhotoListingRemoteKeyEntity
        WHERE
            `key` = :key AND
            user_id = :userId AND
            volume_id = :volumeId AND
            share_id = :shareId AND
            link_id = :linkId
    """
    )
    abstract suspend fun getPhotoListingRemoteKey(
        key: String,
        userId: UserId,
        volumeId: String,
        shareId: String,
        linkId: String,
    ): PhotoListingRemoteKeyEntity?

    @Query(
        """
        SELECT * FROM PhotoListingRemoteKeyEntity
        WHERE
            `key` = :key AND 
            volume_id = :volumeId
        ORDER BY capture_time ASC
        LIMIT 1
    """
    )
    abstract suspend fun getLastRemoteKey(
        key: String,
        volumeId: String,
    ): PhotoListingRemoteKeyEntity?

    @Query(
        """
            DELETE FROM PhotoListingRemoteKeyEntity WHERE `key` = :key AND volume_id = :volumeId
        """
    )
    abstract suspend fun deleteKeys(key: String, volumeId: String)

    suspend fun getPhotoListingRemoteKey(
        key: String,
        volumeId: VolumeId,
        linkId: LinkId,
    ): PhotoListingRemoteKeyEntity? =
        getPhotoListingRemoteKey(
            key = key,
            userId = linkId.userId,
            volumeId = volumeId.id,
            shareId = linkId.shareId.id,
            linkId = linkId.id,
        )
}
