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

import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class StartBackupAfterErrorResolved @Inject constructor(
    private val startBackup: StartBackup,
    private val getErrors: GetErrors,
    private val deleteAllBackupError: DeleteAllBackupError,
) {

    suspend operator fun invoke(userId: UserId, type: BackupErrorType) = coRunCatching {
        val hasErrorType = getErrors(userId).firstOrNull().orEmpty().any { error ->
            error.type == type
        }
        if (hasErrorType) {
            CoreLogger.d(BACKUP, "Restarting backup after $type")
            deleteAllBackupError(userId, type).getOrThrow()
            startBackup(userId)
        } else {
            CoreLogger.d(BACKUP, "Ignore restart, error: $type not found")
        }
    }
}
