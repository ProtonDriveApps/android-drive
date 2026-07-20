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
import me.proton.core.drive.base.data.extension.isErrno
import me.proton.core.drive.base.domain.extension.toApiException
import me.proton.core.network.domain.ApiException
import me.proton.drive.sdk.OperationAbortedException
import me.proton.drive.sdk.ProtonDriveSdkException

fun Throwable.toBackupError(retryable: Boolean = true): BackupError = when (this) {
    is SecurityException -> BackupError.Permissions()
    is ApiException -> toBackupError(retryable)
    is OperationAbortedException -> {
        val errorCause = cause
        if (errorCause is ProtonDriveSdkException) {
            errorCause.toBackupError(retryable)
        } else {
            BackupError.Other(retryable)
        }
    }

    is ProtonDriveSdkException -> when (val error = toApiException()) {
        is ApiException -> error.toBackupError(retryable)
        else -> if (message?.contains("No space left on device") == true) {
            BackupError.LocalStorage()
        } else {
            BackupError.Other(retryable)
        }
    }

    else -> if (isErrno(OsConstants.ENOSPC)) {
        BackupError.LocalStorage()
    } else {
        BackupError.Other(retryable)
    }
}
