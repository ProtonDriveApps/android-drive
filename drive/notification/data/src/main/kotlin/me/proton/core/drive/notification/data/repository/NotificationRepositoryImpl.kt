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
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.notification.data.db.NotificationDatabase
import me.proton.core.drive.notification.data.db.entity.NotificationEventEntity
import me.proton.core.drive.notification.data.db.entity.TaglessNotificationEventEntity
import me.proton.core.drive.notification.data.extension.toChannel
import me.proton.core.drive.notification.data.extension.toNotificationChannelEntity
import me.proton.core.drive.notification.data.extension.toNotificationId
import me.proton.core.drive.notification.domain.entity.Channel
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.repository.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val db: NotificationDatabase,
) : NotificationRepository {

    override suspend fun insertChannels(channels: List<Channel.User>) {
        db.channelDao.insertOrIgnore(
            *channels.map { channel -> channel.toNotificationChannelEntity() }.toTypedArray()
        )
    }

    override suspend fun getAllChannels(userId: UserId): List<Channel.User> =
        db.channelDao.getAll(userId)
            .map { notificationChannelEntity -> notificationChannelEntity.toChannel() }

    override suspend fun removeChannels(userId: UserId) =
        db.channelDao.deleteAll(userId)

    override suspend fun insertNotificationEvent(
        notificationId: NotificationId.User,
        event: Event,
    ) = coRunCatching {
        with(notificationId) {
            val notificationTag = tag
            if (notificationTag == null) {
                db.taglessEventDao.insertOrUpdate(
                    TaglessNotificationEventEntity(
                        userId = channel.userId,
                        channelType = channel.type,
                        notificationId = id,
                        notificationEventId = event.id,
                        notificationEvent = event,
                    )
                )
            } else {
                db.eventDao.insertOrUpdate(
                    NotificationEventEntity(
                        userId = channel.userId,
                        channelType = channel.type,
                        notificationTag = notificationTag,
                        notificationId = id,
                        notificationEventId = event.id,
                        notificationEvent = event,
                    )
                )
            }
        }
    }

    override suspend fun getAllNotificationIds(
        userId: UserId,
    ): List<NotificationId.User> = db.inTransaction {
        val notificationIds = db.eventDao.getAll(userId)
            .map { entity -> entity.toNotificationId() }
        val taglessNotificationIds = db.taglessEventDao.getAll(userId)
            .map { entity -> entity.toNotificationId() }
        notificationIds + taglessNotificationIds
    }

    override suspend fun getAllNotificationEvents(
        notificationId: NotificationId.User,
    ): List<Event> = with(notificationId) {
        val notificationTag = tag
        if (notificationTag == null) {
            db.taglessEventDao.getAll(channel.userId, channel.type, id)
                .map { entity -> entity.notificationEvent }
        } else {
            db.eventDao.getAll(channel.userId, channel.type, notificationTag, id)
                .map { entity -> entity.notificationEvent }
        }
    }

    override suspend fun getNotificationEvent(
        notificationId: NotificationId.User,
        notificationEventId: String,
    ): Event? = with(notificationId) {
        val notificationTag = tag
        if (notificationTag == null) {
            db.taglessEventDao.get(
                userId = channel.userId,
                channelType = channel.type,
                notificationId = id,
                eventId = notificationEventId
            )?.notificationEvent
        } else {
            db.eventDao.get(
                userId = channel.userId,
                channelType = channel.type,
                tag = notificationTag,
                notificationId = id,
                eventId = notificationEventId
            )?.notificationEvent
        }
    }

    override suspend fun removeNotificationEvents(
        notificationId: NotificationId.User,
    ) = with(notificationId) {
        val notificationTag = tag
        if (notificationTag == null) {
            db.taglessEventDao.deleteAll(channel.userId, channel.type, id)
        } else {
            db.eventDao.deleteAll(channel.userId, channel.type, notificationTag, id)
        }
    }

    override suspend fun removeNotificationEvents(userId: UserId) = db.inTransaction {
        db.taglessEventDao.deleteAll(userId)
        db.eventDao.deleteAll(userId)
    }
}

