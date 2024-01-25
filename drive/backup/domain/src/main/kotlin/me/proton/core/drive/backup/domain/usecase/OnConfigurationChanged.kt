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

package me.proton.core.drive.backup.domain.usecase

import me.proton.core.drive.backup.domain.entity.BackupConfiguration
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class OnConfigurationChanged @Inject constructor(
    private val backupManager: BackupManager,
    private val markAllFilesAsIdle: MarkAllFilesAsIdle,
) {
    suspend operator fun invoke(
        previous: BackupConfiguration?,
        current: BackupConfiguration,
    ) = coRunCatching {
        if (previous?.networkType != current.networkType) {
            CoreLogger.d(
                BACKUP,
                "Network type changed from ${previous?.networkType} to ${current.networkType}"
            )
            val folderId = current.folderId
            backupManager.stop(folderId)
            markAllFilesAsIdle(folderId).getOrThrow()
            backupManager.start(folderId)
        }
    }
}
