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

package me.proton.core.drive.device.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.device.data.db.entity.DeviceEntity

@Dao
abstract class DeviceDao : BaseDao<DeviceEntity>() {

    /**
     * By joining [DeviceEntity] table with [LinkEntity] table we ensure that modification of link name would
     * emit list of devices so that new name can be used.
     */
    @Query(
        """
            SELECT DeviceEntity.* FROM DeviceEntity
            LEFT JOIN LinkEntity ON
                DeviceEntity.user_id = LinkEntity.user_id AND
                DeviceEntity.share_id = LinkEntity.share_id AND
                DeviceEntity.link_id = LinkEntity.id
            WHERE
                DeviceEntity.user_id = :userId
            LIMIT :limit OFFSET :offset
        """
    )
    abstract fun getDevicesFlow(
        userId: UserId,
        limit: Int,
        offset: Int,
    ): Flow<List<DeviceEntity>>

    @Query(
        """
            DELETE FROM DeviceEntity
            WHERE user_id = :userId AND volume_id = :volumeId AND share_id = :shareId AND link_id = :linkId
        """
    )
    abstract suspend fun delete(userId: UserId, volumeId: String, shareId: String, linkId: String)

    @Query(
        """
            DELETE FROM DeviceEntity
            WHERE user_id = :userId AND id = :deviceId
        """
    )
    abstract suspend fun delete(userId: UserId, deviceId: String)

    @Query("DELETE FROM DeviceEntity WHERE user_id = :userId")
    abstract suspend fun deleteAll(userId: UserId)
}
