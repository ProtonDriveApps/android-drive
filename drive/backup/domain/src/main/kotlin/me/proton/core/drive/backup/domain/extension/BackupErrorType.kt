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

package me.proton.core.drive.backup.domain.extension

import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.backup.domain.entity.BackupErrorType

fun BackupErrorType.toEventBackupState() = when (this) {
    BackupErrorType.OTHER -> Event.Backup.BackupState.FAILED
    BackupErrorType.CONNECTIVITY -> Event.Backup.BackupState.FAILED_CONNECTIVITY
    BackupErrorType.PERMISSION -> Event.Backup.BackupState.FAILED_PERMISSION
    BackupErrorType.LOCAL_STORAGE -> Event.Backup.BackupState.FAILED_LOCAL_STORAGE
    BackupErrorType.DRIVE_STORAGE -> Event.Backup.BackupState.FAILED_DRIVE_STORAGE
    BackupErrorType.PHOTOS_UPLOAD_NOT_ALLOWED,
    -> Event.Backup.BackupState.FAILED_PHOTOS_UPLOAD_NOT_ALLOWED
}
