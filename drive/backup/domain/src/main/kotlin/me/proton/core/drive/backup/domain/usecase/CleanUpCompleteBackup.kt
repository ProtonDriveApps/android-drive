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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class CleanUpCompleteBackup @Inject constructor(
    private val repository: BackupFileRepository,
    private val logBackupStats: LogBackupStats,
    private val announceEvent: AnnounceEvent,
) {
    suspend operator fun invoke(userId: UserId, folderId: FolderId, bucketId: Int) = coRunCatching {
        if (repository.isBackupCompleteForFolder(userId, bucketId)) {
            CoreLogger.d(BACKUP, "Cleanup completed files for: $bucketId")
            announceEvent(userId, Event.BackupCompleted)
            repository.deleteCompletedFromFolder(userId, bucketId)
        } else {
            CoreLogger.d(BACKUP, "Ignoring cleanup for: $bucketId")
            logBackupStats(userId, bucketId)
        }
    }
}
