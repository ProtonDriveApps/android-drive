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

import android.app.Notification
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.notification.data.extension.id
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.manager.NotificationManager
import me.proton.core.drive.worker.data.usecase.TransferDataNotifier
import javax.inject.Inject

class TransferDataNotificationEventWorkerNotifier @Inject constructor(
    private val createUserNotificationId: CreateUserNotificationId,
    private val builder: TransferDataNotificationBuilder,
    private val notificationManager: NotificationManager,
) : TransferDataNotifier {

    override operator fun invoke(
        userId: UserId,
        event: Event.TransferData,
    ): Result<Pair<NotificationId, Notification>> = runCatching {
        createUserNotificationId(userId, event).let { notificationId ->
            if (notificationManager.hasValidChannel(notificationId).not()) {
                error("Notification $notificationId does not have a valid channel (${notificationId.channel.id})")
            }
            notificationId to builder(notificationId, event).build()
        }
    }

    override fun dismissNotification(notificationId: NotificationId) {
        notificationManager.cancel(notificationId)
    }
}
