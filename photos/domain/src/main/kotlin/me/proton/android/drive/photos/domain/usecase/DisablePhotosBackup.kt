/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.photos.domain.usecase

import me.proton.android.drive.photos.domain.entity.PhotoBackupState
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.entity.Event.Backup.BackupState.PAUSED_DISABLED
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import javax.inject.Inject

class DisablePhotosBackup @Inject constructor(
    private val clearPhotosBackup: ClearPhotosBackup,
    private val backupManager: BackupManager,
    private val announceEvent: AnnounceEvent,
) {
    suspend operator fun invoke(folderId: FolderId) = coRunCatching {
        backupManager.stop(folderId)
        announceEvent(folderId.userId, Event.BackupStopped(folderId, PAUSED_DISABLED))
        clearPhotosBackup(folderId).getOrThrow()
        announceEvent(folderId.userId, Event.BackupDisabled(folderId))
        PhotoBackupState.Disabled
    }
}
