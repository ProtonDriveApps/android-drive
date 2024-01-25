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

import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class DisableBackupForFolder @Inject constructor(
    private val cancelFiles: CancelFiles,
    private val deleteFolder: DeleteFolder,
    private val backupManager: BackupManager,
) {

    suspend operator fun invoke(backupFolder: BackupFolder) = coRunCatching {
        val folderId = backupFolder.folderId
        val bucketId = backupFolder.bucketId
        backupManager.cancelSync(backupFolder)
        val count = cancelFiles(folderId, bucketId).getOrThrow()
        CoreLogger.d(LogTag.BACKUP, "Cancelled $count items")
        deleteFolder(backupFolder).getOrThrow()
        CoreLogger.d(LogTag.BACKUP, "Deleted folder $bucketId")
    }
}
