/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.extension.toEventBackupState
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class PostBackupNotification @Inject constructor(
    private val announceEvent: AnnounceEvent,
    private val getBackupState: GetBackupState,
) {
    suspend operator fun invoke(userId: UserId) = coRunCatching {
        val state = getBackupState(userId).first()
        CoreLogger.d(
            tag = LogTag.BACKUP,
            message = "Backup notification, state: $state",
        )
        state.toEvent()?.let { event ->
            announceEvent(userId, event).getOrThrow()
        }
    }
}

private fun BackupState.toEvent() =
    when (val status = backupStatus) {
        is BackupStatus.Complete -> Event.Backup(
            Event.Backup.BackupState.COMPLETE,
            status.totalBackupPhotos,
            status.totalBackupPhotos
        )

        is BackupStatus.Uncompleted -> Event.Backup(
            Event.Backup.BackupState.UNCOMPLETED,
            status.totalBackupPhotos,
            status.totalBackupPhotos
        )

        is BackupStatus.Failed -> Event.Backup(
            status.errors.map { error ->
                error.type.toEventBackupState()
            }.first(),
            status.totalBackupPhotos,
            status.pendingBackupPhotos,
        )

        is BackupStatus.InProgress -> Event.Backup(
            Event.Backup.BackupState.IN_PROGRESS,
            status.totalBackupPhotos,
            status.pendingBackupPhotos
        )

        null -> null
    }