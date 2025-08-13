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

package me.proton.core.drive.test.eventmanager

import me.proton.core.eventmanager.domain.EventListener
import me.proton.core.eventmanager.domain.EventManager
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.entity.EventId
import me.proton.core.eventmanager.domain.entity.EventMetadata
import me.proton.core.eventmanager.domain.entity.EventsResponse

class TestEventManager(override val config: EventManagerConfig) : EventManager {

    override val isStarted: Boolean
        get() = TODO("Not yet implemented")

    override suspend fun deserializeEventMetadata(
        eventId: EventId,
        response: EventsResponse,
    ): EventMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun getEventResponse(eventId: EventId): EventsResponse {
        TODO("Not yet implemented")
    }

    override suspend fun getLatestEventId(): EventId {
        TODO("Not yet implemented")
    }

    override suspend fun process() {
        TODO("Not yet implemented")
    }

    override suspend fun start() {
        TODO("Not yet implemented")
    }

    override suspend fun stop() {
        TODO("Not yet implemented")
    }

    override suspend fun resume() {
        TODO("Not yet implemented")
    }

    override fun subscribe(eventListener: EventListener<*, *>) {
        TODO("Not yet implemented")
    }

    override suspend fun <R> suspend(block: suspend () -> R): R {
        return block()
    }
}
