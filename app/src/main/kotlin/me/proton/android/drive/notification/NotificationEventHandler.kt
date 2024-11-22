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
import me.proton.android.drive.usecase.notification.CreateUserNotificationId
import me.proton.android.drive.usecase.notification.ShouldCancelNotification
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.handler.EventHandler
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.notification.data.extension.createNotificationId
import me.proton.core.drive.notification.domain.entity.NotificationId
import me.proton.core.drive.notification.domain.usecase.CancelAndRemoveNotification
import me.proton.core.drive.notification.domain.usecase.SaveAndPublishNotification
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class NotificationEventHandler @Inject constructor(
    private val saveAndPublishNotification: SaveAndPublishNotification,
    private val acceptNotificationEvent: AcceptNotificationEvent,
    private val shouldCancelNotification: ShouldCancelNotification,
    private val cancelAndRemoveNotification: CancelAndRemoveNotification,
    private val createUserNotificationId: CreateUserNotificationId,
) : EventHandler {
    private val mutex = Mutex()

    override suspend fun onEvents(userId: UserId, events: List<Event>) {
        events.groupBy { createUserNotificationId(userId, it) }.onEach { (notificationId, events) ->
            onNotificationEvent(notificationId, events)
        }
    }

    override suspend fun onEvent(userId: UserId, event: Event) {
        val notificationId = createUserNotificationId(userId, event)
        onNotificationEvent(
            notificationId = notificationId,
            events = listOf(event),
        )
    }

    override suspend fun onEvent(event: Event) {
        val notificationId = event.createNotificationId()
        onNotificationEvent(
            notificationId = notificationId,
            events = listOf(event),
        )
    }

    private suspend fun onNotificationEvent(
        notificationId: NotificationId,
        events: List<Event>,
    ) {
        val acceptedEvents =
            events.filter { event -> acceptNotificationEvent(notificationId, event) }
        if (acceptedEvents.isNotEmpty()) {
            mutex.withLock {
                saveAndPublishNotification(notificationId, acceptedEvents)
                    .onFailure { error ->
                        CoreLogger.d(
                            LogTag.NOTIFICATION,
                            error,
                            "Save and publish notification failed"
                        )
                    }
                if (shouldCancelNotification(notificationId, acceptedEvents)) {
                    cancelAndRemoveNotification(notificationId)
                }
            }
        }
    }
}
