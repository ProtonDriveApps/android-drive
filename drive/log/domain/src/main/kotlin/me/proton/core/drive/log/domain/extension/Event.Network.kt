/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.log.domain.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.log.domain.entity.Log
import java.util.Locale

internal fun Event.Network.toLog(userId: UserId): Log =
    Log(
        userId = userId,
        level = level,
        message = message,
        moreContent = moreContent,
        creationTime = occurredAt,
        origin = Log.Origin.EVENT_NETWORK,
    )

internal val Event.Network.message: String get() =
    """
        ${response.code} (${response.occurredAt.value - request.occurredAt.value}ms) ->
        ${request.method.uppercase(Locale.US)} ${request.urlPath}
    """.trimIndent().replace("\n", " ")

internal val Event.Network.moreContent: String get() =
    """
        Request: ${request.method.uppercase(Locale.US)} ${request.urlPath}
        Response: ${response.code} ${response.message}
            ${response.jsonBody}
    """.trimIndent()

internal val Event.Network.level: Log.Level get() = when (response.code) {
    400, 404, 422 -> Log.Level.ERROR
    in 400..499 -> Log.Level.WARNING
    in 500..599 -> Log.Level.WARNING
    else -> Log.Level.NORMAL
}
