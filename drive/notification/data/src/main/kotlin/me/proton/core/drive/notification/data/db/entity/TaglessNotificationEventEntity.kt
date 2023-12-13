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
package me.proton.core.drive.notification.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.data.db.Column.CHANNEL_TYPE
import me.proton.core.drive.base.data.db.Column.NOTIFICATION_EVENT
import me.proton.core.drive.base.data.db.Column.NOTIFICATION_EVENT_ID
import me.proton.core.drive.base.data.db.Column.NOTIFICATION_ID
import me.proton.core.drive.base.data.db.Column.TYPE
import me.proton.core.drive.base.data.db.Column.USER_ID
import me.proton.core.drive.notification.domain.entity.Channel

@Entity(
    primaryKeys = [USER_ID, CHANNEL_TYPE, NOTIFICATION_ID, NOTIFICATION_EVENT_ID],
    foreignKeys = [
        ForeignKey(
            entity = NotificationChannelEntity::class,
            parentColumns = [USER_ID, TYPE],
            childColumns = [USER_ID, CHANNEL_TYPE],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = [USER_ID, CHANNEL_TYPE, NOTIFICATION_ID])
    ]
)
data class TaglessNotificationEventEntity(
    @ColumnInfo(name = USER_ID)
    val userId: UserId,
    @ColumnInfo(name = CHANNEL_TYPE)
    val channelType: Channel.Type,
    @ColumnInfo(name = NOTIFICATION_ID)
    val notificationId: Int,
    @ColumnInfo(name = NOTIFICATION_EVENT_ID)
    val notificationEventId: String,
    @ColumnInfo(name = NOTIFICATION_EVENT)
    val notificationEvent: Event
)
