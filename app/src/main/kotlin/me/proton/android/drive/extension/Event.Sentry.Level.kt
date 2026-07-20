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

package me.proton.android.drive.extension

import io.sentry.SentryLevel
import me.proton.core.drive.announce.event.domain.entity.Event

val Event.Sentry.Level.sentryLevel: SentryLevel get() = when (this) {
    Event.Sentry.Level.DEBUG -> SentryLevel.DEBUG
    Event.Sentry.Level.INFO -> SentryLevel.INFO
    Event.Sentry.Level.WARNING -> SentryLevel.WARNING
    Event.Sentry.Level.ERROR -> SentryLevel.ERROR
    Event.Sentry.Level.FATAL -> SentryLevel.FATAL
}
