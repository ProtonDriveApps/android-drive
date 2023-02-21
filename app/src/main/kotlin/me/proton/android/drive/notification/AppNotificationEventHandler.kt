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

package me.proton.android.drive.notification

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.android.drive.usecase.notification.AcceptNotificationEvent
import me.proton.android.drive.usecase.notification.ShouldCancelNotification
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.notification.data.extension.createNotificationId
import me.proton.core.drive.notification.data.extension.tag
import me.proton.core.drive.notification.domain.entity.NotificationEvent
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.handler.NotificationEventHandler
import me.proton.core.drive.notification.domain.usecase.CancelAndRemoveNotification
import me.proton.core.drive.notification.domain.usecase.SaveAndPublishNotification
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class AppNotificationEventHandler @Inject constructor(
    private val saveAndPublishNotification: SaveAndPublishNotification,
    private val acceptNotificationEvent: AcceptNotificationEvent,
    private val shouldCancelNotification: ShouldCancelNotification,
    private val cancelAndRemoveNotification: CancelAndRemoveNotification,
) : NotificationEventHandler {
    private val mutex = Mutex()

    override fun createNotificationId(userId: UserId, notificationEvent: NotificationEvent) = when (notificationEvent) {
        is NotificationEvent.Download -> notificationEvent.createNotificationId(userId).copy(
            tag = "${notificationEvent.tag}_${notificationEvent.downloadId}"
        )
        else -> notificationEvent.createNotificationId(userId)
    }

    override suspend fun onNotificationEvent(
        notificationId: NotificationId,
        event: NotificationEvent,
    ) = mutex.withLock {
        if (acceptNotificationEvent(notificationId, event)) {
            saveAndPublishNotification(notificationId, event)
                .onFailure { error ->
                    CoreLogger.d(LogTag.NOTIFICATION, error, "Save and publish notification failed")
                }
            if (shouldCancelNotification(notificationId, event)) {
                cancelAndRemoveNotification(notificationId)
            }
        }
    }
}
