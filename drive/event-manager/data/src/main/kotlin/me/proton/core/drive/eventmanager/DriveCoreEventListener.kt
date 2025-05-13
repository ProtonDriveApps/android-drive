/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.eventmanager

import kotlinx.serialization.json.Json
import me.proton.core.drive.base.data.api.Dto
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.eventmanager.api.response.DriveCoreEventDto
import me.proton.core.drive.eventmanager.entity.DriveCoreEvent
import me.proton.core.drive.eventmanager.usecase.OnUpdateDriveCoreEvent
import me.proton.core.eventmanager.domain.EventListener
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.entity.Action
import me.proton.core.eventmanager.domain.entity.Event
import me.proton.core.eventmanager.domain.entity.EventsResponse
import javax.inject.Inject

class DriveCoreEventListener @Inject constructor(
    private val onUpdateDriveCoreEvent: OnUpdateDriveCoreEvent,
) : EventListener<String, DriveCoreEvent>() {
    internal var onFailure: (Throwable, String) -> Unit = { error, body ->
        error.log(LogTag.EVENTS, "Cannot parse event from response: $body")
    }
    override val order: Int = 3
    override val type: Type = Type.Core

    override suspend fun deserializeEvents(
        config: EventManagerConfig,
        response: EventsResponse
    ): List<Event<String, DriveCoreEvent>>? {
        return runCatching {
            json.decodeFromString<DriveCoreEventDto>(response.body).let { driveCoreEventDto ->
                driveCoreEventDto.driveShareRefresh?.eventAction?.let { eventAction ->
                    listOf(
                        Event(
                            action = eventAction,
                            key = config.userId.id,
                            entity = DriveCoreEvent(driveShareRefreshAction = eventAction),
                        )
                    )
                }
            }
        }.onFailure { error -> onFailure(error, response.body) }.getOrNull()
    }

    // We need to do things (like deleting local content or filter entities) which could result in a deadlock or block
    // the database for too long so we prefer to leave the transaction responsibility to the use-cases
    override suspend fun <R> inTransaction(block: suspend () -> R): R = block()

    override suspend fun onUpdate(config: EventManagerConfig, entities: List<DriveCoreEvent>) =
        onUpdateDriveCoreEvent(config.userId, entities)

    private val DriveCoreEventDto.DriveShareRefresh.eventAction: Action? get() = when (action) {
        0 -> Action.Delete
        1 -> Action.Create
        2 -> Action.Update
        3 -> Action.Partial
        else -> null
    }

    companion object {
        internal val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
            classDiscriminator = Dto.EVENT_TYPE
        }
    }
}
