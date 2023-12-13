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
import me.proton.core.drive.notification.data.db.entity.TaglessNotificationEventEntity
import me.proton.core.drive.notification.domain.entity.Channel

@Dao
abstract class TaglessNotificationEventDao : BaseDao<TaglessNotificationEventEntity>() {

    @Query("""
        SELECT * FROM TaglessNotificationEventEntity WHERE
            user_id = :userId AND channel_type = :channelType AND
            notification_id = :notificationId AND notification_event_id = :eventId
    """)
    abstract suspend fun get(
        userId: UserId, channelType: Channel.Type, notificationId: Int, eventId: String
    ): TaglessNotificationEventEntity?

    @Query("""
        SELECT * FROM TaglessNotificationEventEntity WHERE
            user_id = :userId AND channel_type = :channelType AND notification_id = :id
    """)
    abstract suspend fun getAll(
        userId: UserId,
        channelType: Channel.Type,
        id: Int
    ): List<TaglessNotificationEventEntity>

    @Query("""
        SELECT * FROM TaglessNotificationEventEntity WHERE user_id = :userId
    """)
    abstract suspend fun getAll(userId: UserId): List<TaglessNotificationEventEntity>

    @Query("""
        DELETE FROM TaglessNotificationEventEntity WHERE
            user_id = :userId AND channel_type = :channelType AND notification_id = :id
    """)
    abstract suspend fun deleteAll(userId: UserId, channelType: Channel.Type, id: Int)

    @Query("""
        DELETE FROM TaglessNotificationEventEntity WHERE user_id = :userId
    """)
    abstract suspend fun deleteAll(userId: UserId)
}
