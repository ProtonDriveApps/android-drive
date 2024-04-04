/*
 * Copyright (c) 2023-2024 Proton AG.
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

import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.extension.toEventBackupState
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class StopBackup @Inject constructor(
    private val manager: BackupManager,
    private val addBackupError: AddBackupError,
    private val logBackupStats: LogBackupStats,
    private val announceEvent: AnnounceEvent,
    private val getAllFolders: GetAllFolders,
    private val markAllEnqueuedAsReady: MarkAllEnqueuedAsReady,
) {

    suspend operator fun invoke(folderId: FolderId, error: BackupError) = coRunCatching {
        CoreLogger.d(BACKUP, "Stopping after: $error")
        announceEvent(folderId.userId, Event.BackupStopped(folderId, error.type.toEventBackupState()))
        logBackupStats(folderId)
        addBackupError(folderId, error).getOrThrow()
        getAllFolders(folderId).getOrThrow().forEach { backupFolder ->
            markAllEnqueuedAsReady(backupFolder).onSuccess { count ->
                CoreLogger.d(BACKUP, "Mark $count files as ready")
            }.getOrThrow()
        }
        manager.stop(folderId)
    }
}
