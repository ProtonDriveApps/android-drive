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
package me.proton.core.drive.notification.domain.handler

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.entity.NotificationId

interface NotificationEventHandler {
    fun createUserNotificationId(userId: UserId, notificationEvent: NotificationEvent): NotificationId.User

    fun createAppNotificationId(notificationEvent: NotificationEvent): NotificationId.App

    suspend fun onNotificationEvent(
        notificationId: NotificationId,
        event: NotificationEvent,
    )
}
