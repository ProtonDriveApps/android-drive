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
package me.proton.core.drive.notification.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.notification.data.db.entity.NotificationChannelEntity

@Dao
abstract class NotificationChannelDao : BaseDao<NotificationChannelEntity>() {
    @Query("""
        SELECT * FROM NotificationChannelEntity WHERE user_id = :userId
    """)
    abstract suspend fun getAll(userId: UserId): List<NotificationChannelEntity>

    @Query("""
        DELETE FROM NotificationChannelEntity WHERE user_id = :userId
    """)
    abstract suspend fun deleteAll(userId: UserId)
}
