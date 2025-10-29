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

package me.proton.core.drive.log.domain.handler

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.handler.EventHandler
import me.proton.core.drive.log.domain.entity.Log
import me.proton.core.drive.log.domain.extension.toLog
import me.proton.core.drive.log.domain.usecase.InsertLogs
import javax.inject.Inject

class LogEventHandler @Inject constructor(
    private val insertLogs: InsertLogs,
) : EventHandler {
    override suspend fun onEvents(userId: UserId, events: List<Event>) {
        events.mapNotNull { event -> event.log(userId) }.takeIf { it.isNotEmpty() }?.let { logs ->
            insertLogs(logs)
        }
    }

    override suspend fun onEvent(userId: UserId, event: Event) {
        event.log(userId)?.let { log ->
            insertLogs(listOf(log))
        }
    }

    private fun Event.log(userId: UserId): Log? = when (this) {
        is Event.Download -> toLog(userId)
        is Event.Upload -> toLog(userId)
        is Event.Throwable -> toLog(userId)
        is Event.Network -> toLog(userId)
        is Event.Logger -> toLog(userId)
        is Event.Screen -> toLog(userId)
        is Event.ApplicationState -> toLog(userId)
        is Event.Workers -> toLog(userId)
        is Event.Backup -> toLog(userId)
        is Event.BackupFolder -> toLog(userId)
        is Event.BackupCompleted -> toLog(userId)
        is Event.BackupDisabled -> toLog(userId)
        is Event.BackupEnabled -> toLog(userId)
        is Event.BackupStarted -> toLog(userId)
        is Event.BackupStopped -> toLog(userId)
        is Event.BackupSync -> toLog(userId)
        is Event.StorageFull -> toLog(userId)
        is Event.ForcedSignOut -> toLog(userId)
        is Event.NoSpaceLeftOnDevice -> toLog(userId)
        is Event.SignatureVerificationFailed -> toLog(userId)
        is Event.TransferData -> toLog(userId)
        is Event.Album -> toLog(userId)
        is Event.UploadSpeed -> toLog(userId)
        is Event.DownloadSpeed -> toLog(userId)
    }
}
