/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.android.drive.sentry

import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.protocol.Message
import me.proton.android.drive.extension.sentryLevel
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.handler.EventHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentryEventHandler @Inject constructor() : EventHandler {

    override suspend fun onEvent(userId: UserId, event: Event) {
        when (event) {
            is Event.Sentry -> processSentryEvent(event)
            else -> Unit
        }
    }

    fun processSentryEvent(event: Event.Sentry) {
        Sentry.captureEvent(
            SentryEvent().apply {
                level = event.level.sentryLevel
                message = Message().apply {
                    formatted = event.message
                }
                event.tags.forEach { (key, value) ->
                    setTag(key, value)
                }
            }
        )
    }
}
