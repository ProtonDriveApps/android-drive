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
package me.proton.core.drive.notification.data.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.notification.data.db.NotificationDatabase
import me.proton.core.drive.notification.data.db.entity.NotificationEventEntity
import me.proton.core.drive.notification.data.extension.toChannel
import me.proton.core.drive.notification.data.extension.toNotificationChannelEntity
import me.proton.core.drive.notification.data.extension.toNotificationId
import me.proton.core.drive.notification.domain.entity.Channel
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.repository.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val db: NotificationDatabase,
) : NotificationRepository {

    override suspend fun insertChannels(channels: List<Channel>) {
        db.channelDao.insertOrIgnore(
            *channels.map { channel -> channel.toNotificationChannelEntity() }.toTypedArray()
        )
    }

    override suspend fun getAllChannels(userId: UserId): List<Channel> =
        db.channelDao.getAll(userId).map { notificationChannelEntity -> notificationChannelEntity.toChannel() }

    override suspend fun removeChannels(userId: UserId) =
        db.channelDao.deleteAll(userId)

    override suspend fun insertNotificationEvent(notificationId: NotificationId, notificationEvent: NotificationEvent) =
        coRunCatching {
            with (notificationId) {
                db.eventDao.insertOrUpdate(
                    NotificationEventEntity(
                        userId = channel.userId,
                        channelType = channel.type,
                        notificationTag = tag,
                        notificationId = id,
                        notificationEventId = notificationEvent.id,
                        notificationEvent = notificationEvent,
                    )
                )
            }
        }

    override suspend fun getAllNotificationIds(userId: UserId): List<NotificationId> =
        db.eventDao.getAll(userId).map { notificationEventEntity -> notificationEventEntity.toNotificationId() }

    override suspend fun getAllNotificationEvents(notificationId: NotificationId): List<NotificationEvent> =
        with (notificationId) {
            db.eventDao.getAll(channel.userId, channel.type, tag, id).map { notificationEventEntity ->
                notificationEventEntity.notificationEvent
            }
        }

    override suspend fun getNotificationEvent(
        notificationId: NotificationId,
        notificationEventId: String,
    ): NotificationEvent? = with (notificationId) {
        db.eventDao.get(channel.userId, channel.type, tag, id, notificationEventId)?.notificationEvent
    }

    override suspend fun removeNotificationEvents(notificationId: NotificationId) =
        with (notificationId) {
            db.eventDao.deleteAll(channel.userId, channel.type, tag, id)
        }

    override suspend fun removeNotificationEvents(userId: UserId) =
        db.eventDao.deleteAll(userId)
}
