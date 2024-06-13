/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.android.drive.log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.util.kotlin.Logger
import me.proton.core.util.kotlin.orEmpty
import kotlin.coroutines.CoroutineContext

open class UserLogger(
    private val asyncAnnounceEvent: AsyncAnnounceEvent,
    accountManager: AccountManager,
    coroutineContext: CoroutineContext,
) : Logger {
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val userId = accountManager
        .getPrimaryUserId()
        .stateIn(coroutineScope, SharingStarted.Eagerly, null)

    override fun d(tag: String, message: String) = Unit

    override fun d(tag: String, e: Throwable, message: String) {
        e.announceThrowableEvent(message, Event.Logger.Level.DEBUG)
    }

    override fun e(tag: String, message: String) = Unit

    override fun e(tag: String, e: Throwable) {
        e.announceThrowableEvent(e.message.orEmpty(), Event.Logger.Level.ERROR)
    }

    override fun e(tag: String, e: Throwable, message: String) {
        e.announceThrowableEvent(message, Event.Logger.Level.ERROR)
    }

    override fun i(tag: String, message: String) {
        announceLoggerEvent(tag, message, Event.Logger.Level.INFO)
    }

    override fun i(tag: String, e: Throwable, message: String) {
        e.announceThrowableEvent(message, Event.Logger.Level.INFO)
    }

    override fun v(tag: String, message: String) = Unit

    override fun v(tag: String, e: Throwable, message: String) {
        e.announceThrowableEvent(message, Event.Logger.Level.VERBOSE)
    }

    override fun w(tag: String, message: String) = Unit

    override fun w(tag: String, e: Throwable) {
        e.announceThrowableEvent(e.message.orEmpty(), Event.Logger.Level.WARNING)
    }

    override fun w(tag: String, e: Throwable, message: String) {
        e.announceThrowableEvent(message, Event.Logger.Level.WARNING)
    }

    private fun Throwable.announceThrowableEvent(message: String, level: Event.Logger.Level) {
        announceEvent(
            event = Event.Throwable(
                message = message,
                throwable = this,
                level = level,
            )
        )
    }

    private fun announceLoggerEvent(tag: String, message: String, level: Event.Logger.Level) {
        announceEvent(
            event = Event.Logger(
                tag = tag,
                message = message,
                level = level,
            )
        )
    }

    private fun announceEvent(event: Event) = userId.value?.let { userId ->
        asyncAnnounceEvent(
            userId = userId,
            event = event,
        )
    }
}
