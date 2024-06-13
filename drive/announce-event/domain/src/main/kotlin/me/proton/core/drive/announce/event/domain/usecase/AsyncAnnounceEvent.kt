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

package me.proton.core.drive.announce.event.domain.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.handler.EventHandler
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class AsyncAnnounceEvent @Inject constructor(
    private val eventHandlers: @JvmSuppressWildcards Set<EventHandler>,
    coroutineContext: CoroutineContext,
) {
    private val coroutineScope = CoroutineScope(coroutineContext)

    operator fun invoke(
        userId: UserId,
        event: Event,
    ) = coroutineScope.launch {
        val deferred = eventHandlers.map { handler ->
            async {
                coRunCatching {
                    handler.onEvent(userId, event)
                }.onFailure { error ->
                    CoreLogger.d(
                        LogTag.ANNOUNCE_EVENT,
                        error,
                        "Error during broadcast of ${event.id} to $handler"
                    )
                }
            }
        }
        try {
            deferred.awaitAll()
        } catch (e: CancellationException) {
            CoreLogger.d(LogTag.ANNOUNCE_EVENT, e, "Handling event ${event.id} is cancelled")
            cancel()
        }
    }
}
