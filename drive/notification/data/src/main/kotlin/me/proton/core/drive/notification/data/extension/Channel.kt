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

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import me.proton.core.drive.notification.data.db.entity.NotificationChannelEntity
import me.proton.core.drive.notification.domain.entity.Channel
import me.proton.core.drive.base.presentation.R as BasePresentation

fun Channel.buildNotificationChannelCompat(appContext: Context, groupId: String?): NotificationChannelCompat {
    val (name, description) = when (type) {
        Channel.Type.TRANSFER ->
            appContext.getString(BasePresentation.string.notification_channel_transfer_title) to
                appContext.getString(BasePresentation.string.notification_channel_transfer_description)
    }
    return NotificationChannelCompat.Builder(id, NotificationManagerCompat.IMPORTANCE_DEFAULT)
        .setName(name)
        .setDescription(description)
        .setGroup(groupId)
        .build()
}

val Channel.id: String get() = "CHANNEL_ID_${type.name.uppercase()}_${userId.id}"

fun Channel.toNotificationChannelEntity() =
    NotificationChannelEntity(
        userId = userId,
        type = type,
    )
