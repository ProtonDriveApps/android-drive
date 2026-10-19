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

package me.proton.core.drive.recently.accessed.data.handler

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.handler.EventHandler
import me.proton.core.drive.recently.accessed.domain.usecase.RecordRecentlyAccessed
import javax.inject.Inject

class RecentlyAccessedEventHandler @Inject constructor(
    private val recordRecentlyAccessed: RecordRecentlyAccessed,
) : EventHandler {

    override suspend fun onEvent(userId: UserId, event: Event) {
        when (event) {
            is Event.RecentlyAccessed.FileInfoOpened -> recordRecentlyAccessed(
                userId = userId,
                linkIds = listOf(event.linkId),
                accessTime = event.occurredAt,
            )
            is Event.RecentlyAccessed.PreviewOpened -> recordRecentlyAccessed(
                userId = userId,
                linkIds = listOf(event.fileId),
                accessTime = event.occurredAt,
            )
            is Event.RecentlyAccessed.MarkedAsOffline -> recordRecentlyAccessed(
                userId = userId,
                linkIds = listOf(event.linkId),
                accessTime = event.occurredAt,
            )
            is Event.RecentlyAccessed.FilesDownloaded -> recordRecentlyAccessed(
                userId = userId,
                linkIds = event.fileIds,
                accessTime = event.occurredAt,
            )
            else -> Unit
        }
    }
}
