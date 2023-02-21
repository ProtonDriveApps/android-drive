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
package me.proton.core.drive.notification.domain.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.notification.domain.entity.Channel
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.entity.NotificationId

interface NotificationRepository {
    /**
     * Inserts channels into cache
     * Channels cannot be updated so if channel already exists it will be ignored
     */
    suspend fun insertChannels(channels: List<Channel>)

    /**
     * Gets list of all channels for given user
     */
    suspend fun getAllChannels(userId: UserId): List<Channel>

    /**
     * Removes channels from cache
     */
    suspend fun removeChannels(userId: UserId)

    /**
     * Inserts or updates [NotificationEvent] in cache based on [NotificationId]
     */
    suspend fun insertNotificationEvent(notificationId: NotificationId, notificationEvent: NotificationEvent): Result<Unit>

    /**
     * Gets all [NotificationId] for a given user
     */
    suspend fun getAllNotificationIds(userId: UserId): List<NotificationId>

    /**
     * Gets all [NotificationEvent] for a given [NotificationId]
     */
    suspend fun getAllNotificationEvents(notificationId: NotificationId): List<NotificationEvent>

    /**
     * Get [NotificationEvent] for given [NotificationId] and [NotificationEvent] id
     * Note that [NotificationId] does not necessarily provides single [NotificationEvent] but when provided with
     * [NotificationEvent] id then it is unique
     */
    suspend fun getNotificationEvent(notificationId: NotificationId, notificationEventId: String): NotificationEvent?

    /**
     * Removes all [NotificationEvent] for a given [NotificationId]
     */
    suspend fun removeNotificationEvents(notificationId: NotificationId)

    /**
     * Removes all [NotificationEvent] for a given user
     */
    suspend fun removeNotificationEvents(userId: UserId)
}
