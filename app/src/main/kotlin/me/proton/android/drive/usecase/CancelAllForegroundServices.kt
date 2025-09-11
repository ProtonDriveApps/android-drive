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

package me.proton.android.drive.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.upload.domain.usecase.CancelUploadForegroundServices
import javax.inject.Inject

class CancelAllForegroundServices @Inject constructor(
    private val cancelUploadForegroundServices: CancelUploadForegroundServices,
    private val backupManager: BackupManager
) {

    suspend operator fun invoke(userId: UserId) {
        cancelUploadForegroundServices(userId)
        backupManager.cancelForegroundWork(userId)
    }
}
