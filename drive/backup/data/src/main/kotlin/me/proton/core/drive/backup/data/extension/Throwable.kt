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

package me.proton.core.drive.backup.data.extension

import android.system.OsConstants
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.base.domain.api.ProtonApiCode.EXCEEDED_QUOTA
import me.proton.core.drive.base.data.extension.isErrno
import me.proton.core.drive.base.domain.api.ProtonApiCode.PHOTO_MIGRATION
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.hasProtonErrorCode

fun Throwable.toBackupError(retryable: Boolean = true): BackupError = when (this) {
    is SecurityException -> BackupError.Permissions()
    is ApiException -> when {
        hasProtonErrorCode(EXCEEDED_QUOTA) -> BackupError.DriveStorage()
        hasProtonErrorCode(PHOTO_MIGRATION) -> BackupError.Migration()
        else -> BackupError.Other(retryable)
    }

    else -> if (isErrno(OsConstants.ENOSPC)) {
        BackupError.LocalStorage()
    } else {
        BackupError.Other(retryable)
    }
}
