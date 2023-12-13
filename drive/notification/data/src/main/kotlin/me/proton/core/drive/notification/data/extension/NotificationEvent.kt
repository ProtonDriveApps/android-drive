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
package me.proton.core.drive.notification.data.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.notification.domain.entity.Channel
import me.proton.core.drive.notification.domain.entity.NotificationId

val Event.tag: String get() = "NOTIFICATION_TAG_${this.javaClass.simpleName.uppercase()}"

fun Event.createNotificationId(userId: UserId, type: Channel.Type = Channel.Type.TRANSFER) =
    NotificationId.User(
        channel = Channel.User(userId, type),
        tag = tag,
        id = 1,
    )

fun Event.createNotificationId(type: Channel.Type = Channel.Type.WARNING) =
    NotificationId.App(
        channel = Channel.App(type),
        tag = tag,
        id = 1,
    )
