/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.eventmanager.usecase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.core.drive.base.domain.coroutines.ApplicationScope
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.log.LogTag.EVENTS
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.handler.LinkNotFoundHandler
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import javax.inject.Provider

/**
 * Cleans up a link the backend no longer has the same way as a delete event would, in the
 * application scope rather than in the caller's coroutine (see [LinkNotFoundHandler.onLinkNotFound]).
 *
 * [HandleOnDeleteEvent] is provided lazily because it depends on use cases that depend on
 * [LinkNotFoundHandler].
 */
class LinkNotFoundHandlerImpl @Inject constructor(
    private val handleOnDeleteEvent: Provider<HandleOnDeleteEvent>,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : LinkNotFoundHandler {

    override fun onLinkNotFound(linkId: LinkId) {
        CoreLogger.i(EVENTS, "Link ${linkId.id.logId()} no longer exists, deleting it locally")
        applicationScope.launch {
            handleOnDeleteEvent.get()(listOf(linkId))
                .onFailure { error ->
                    CoreLogger.w(EVENTS, error, "Deleting link ${linkId.id.logId()} locally failed")
                }
        }
    }
}
