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

package me.proton.core.drive.test.handler

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.handler.EventHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryEventHandler @Inject constructor(): EventHandler {

    var events = mutableMapOf<UserId?, List<Event>>()
    override suspend fun onEvent(userId: UserId, event: Event) {
        events[userId] = events.getOrElse(userId) {emptyList()} + event
    }

    override suspend fun onEvent(event: Event) {
        events[null] = events.getOrElse(null) {emptyList()} + event
    }
}
