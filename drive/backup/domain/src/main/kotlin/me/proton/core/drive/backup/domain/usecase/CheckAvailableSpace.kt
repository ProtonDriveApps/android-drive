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

import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.base.domain.extension.availableSpace
import me.proton.core.drive.base.domain.extension.throwOnFailure
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.user.domain.entity.User
import javax.inject.Inject

class CheckAvailableSpace @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val getAllFolders: GetAllFolders,
    private val stopBackup: StopBackup,
) {
    suspend operator fun invoke(user: User) = coRunCatching {
        val availableSpace = user.availableSpace
        if (availableSpace <= configurationProvider.backupLeftSpace) {
            getAllFolders(user.userId)
                .getOrThrow()
                .map { backupFolder -> backupFolder.folderId }
                .distinct()
                .map { folderId ->
                    stopBackup(folderId, BackupError.DriveStorage())
                }.throwOnFailure { count ->
                    "Failed to stop backup, for $count folders"
                }
        }
    }
}
