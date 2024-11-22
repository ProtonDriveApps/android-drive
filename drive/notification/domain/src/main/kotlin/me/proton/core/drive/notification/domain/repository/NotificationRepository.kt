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
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.notification.domain.entity.Channel
import me.proton.core.drive.notification.domain.entity.NotificationId

interface NotificationRepository {
    /**
     * Inserts channels into cache
     * Channels cannot be updated so if channel already exists it will be ignored
     */
    suspend fun insertChannels(channels: List<Channel.User>)

    /**
     * Gets list of all channels for given user
     */
    suspend fun getAllChannels(userId: UserId): List<Channel.User>

    /**
     * Removes channels from cache
     */
    suspend fun removeChannels(userId: UserId)

    /**
     * Inserts or updates [Event] in cache based on [NotificationId]
     */
    suspend fun insertNotificationEvent(notificationId: NotificationId.User, event: Event): Result<Unit>

    /**
     * Inserts or updates list of [Event] in cache based on [NotificationId]
     */
    suspend fun insertNotificationEvents(notificationId: NotificationId.User, events: List<Event>): Result<Unit>

    /**
     * Gets all [NotificationId] for a given user
     */
    suspend fun getAllNotificationIds(userId: UserId): List<NotificationId.User>

    /**
     * Gets all [Event] for a given [NotificationId]
     */
    suspend fun getAllNotificationEvents(notificationId: NotificationId.User): List<Event>

    /**
     * Get [Event] for given [NotificationId] and [Event] id
     * Note that [NotificationId] does not necessarily provides single [Event] but when provided with
     * [Event] id then it is unique
     */
    suspend fun getNotificationEvent(notificationId: NotificationId.User, notificationEventId: String): Event?

    /**
     * Removes all [Event] for a given [NotificationId]
     */
    suspend fun removeNotificationEvents(notificationId: NotificationId.User)

    /**
     * Removes all [Event] for a given user
     */
    suspend fun removeNotificationEvents(userId: UserId)

    /**
     * Checks if user has rejected notification permission rationale
     */
    suspend fun hasUserRejectNotificationPermissionRationale(userId: UserId): Boolean?

    /**
     * Sets user reject notification permission rationale
     */
    suspend fun setUserRejectNotificationPermissionRationale(userId: UserId, value: Boolean)
}
