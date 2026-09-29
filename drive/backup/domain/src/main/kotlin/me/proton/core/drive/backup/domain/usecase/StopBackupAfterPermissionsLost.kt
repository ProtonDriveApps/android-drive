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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class StopBackupAfterPermissionsLost @Inject constructor(
    private val stopBackup: StopBackup,
    private val getAllFolders: GetAllFolders,
    private val getErrors: GetErrors,
) {

    suspend operator fun invoke(userId: UserId) = coRunCatching {
        getAllFolders(userId)
            .getOrThrow()
            .map { backupFolder -> backupFolder.folderId }
            .distinct()
            .forEach { folderId ->
                val alreadyStopped = getErrors(folderId).firstOrNull().orEmpty().any { error ->
                    error.type == BackupErrorType.PERMISSION
                }
                if (alreadyStopped) {
                    CoreLogger.d(BACKUP, "Ignore stop, backup already stopped for permissions")
                } else {
                    CoreLogger.i(BACKUP, "Stopping backup, permissions were lost")
                    stopBackup(folderId, BackupError.Permissions()).getOrThrow()
                }
            }
    }
}
