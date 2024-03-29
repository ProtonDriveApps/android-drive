/*
 * Copyright (c) 2023 Proton AG.
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

import kotlinx.coroutines.flow.first
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

class TogglePhotosBackup @Inject constructor(
    private val backupManager: BackupManager,
    private val enablePhotosBackup: EnablePhotosBackup,
    private val disablePhotosBackup: DisablePhotosBackup,
) {
    suspend operator fun invoke(folderId: FolderId) = coRunCatching {
        if (backupManager.isEnabled(folderId).first()) {
            disablePhotosBackup(folderId).getOrThrow()
        } else {
            enablePhotosBackup(folderId).getOrThrow()
        }
    }
}
