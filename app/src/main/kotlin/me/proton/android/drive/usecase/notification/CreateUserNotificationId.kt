/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.usecase.notification

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.notification.data.extension.createNotificationId
import me.proton.core.drive.notification.data.extension.tag
import me.proton.core.drive.notification.domain.entity.Channel
import me.proton.core.drive.notification.domain.entity.TaglessNotificationId
import me.proton.core.drive.notification.domain.extension.createTaglessNotificationId
import javax.inject.Inject

class CreateUserNotificationId @Inject constructor() {
    operator fun invoke(
        userId: UserId,
        event: Event,
    ) = when (event) {
        is Event.Download -> event.createNotificationId(userId).copy(
            tag = "${event.tag}_${event.downloadId}"
        )

        // Foreground service in worker do not use tag
        is Event.TransferData -> TaglessNotificationId.UPLOAD.createTaglessNotificationId(
            userId
        )

        is Event.Backup -> event.createNotificationId(
            userId,
            Channel.Type.BACKGROUND
        )

        else -> event.createNotificationId(userId)
    }
}
