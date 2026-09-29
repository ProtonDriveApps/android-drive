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

import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.base.data.extension.isNoSpaceLeftOnDevice
import me.proton.core.drive.base.domain.extension.firstErrorOrNull
import me.proton.core.drive.base.domain.extension.toApiException
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.linkId
import me.proton.core.network.domain.ApiException
import me.proton.drive.sdk.OperationAbortedException
import me.proton.drive.sdk.ProtonDriveSdkException
import me.proton.drive.sdk.ProtonSdkError

// A missing link alone does not mean the backup folder is gone, the error has to name the folder
private fun ProtonDriveSdkException.isFolderNotFound(folderId: FolderId): Boolean =
    error.firstErrorOrNull { sdkError -> sdkError.missingLinkId == folderId.id } != null

private val ProtonSdkError.missingLinkId: String?
    get() = (additionalData as? ProtonSdkError.Data.NodeNotFound)
        ?.nodeUid
        ?.linkId

internal val Throwable.isMissingMediaLocationPermission: Boolean
    get() = this is UnsupportedOperationException &&
            message?.contains(ACCESS_MEDIA_LOCATION) == true

fun Throwable.toBackupError(folderId: FolderId, retryable: Boolean = true): BackupError = when (this) {
    is SecurityException -> BackupError.Permissions()
    is UnsupportedOperationException -> if (isMissingMediaLocationPermission) {
        BackupError.Permissions()
    } else {
        BackupError.Other(retryable)
    }
    is ApiException -> toBackupError(retryable)
    is OperationAbortedException -> {
        val errorCause = cause
        when {
            errorCause is ProtonDriveSdkException -> errorCause.toBackupError(folderId, retryable)
            isNoSpaceLeftOnDevice -> BackupError.LocalStorage()
            else -> BackupError.Other(retryable)
        }
    }

    is ProtonDriveSdkException -> if (isFolderNotFound(folderId)) {
        BackupError.FolderNotFound()
    } else {
        when (val error = toApiException()) {
            is ApiException -> error.toBackupError(retryable)
            else -> if (isNoSpaceLeftOnDevice) {
                BackupError.LocalStorage()
            } else {
                BackupError.Other(retryable)
            }
        }
    }

    else -> if (isNoSpaceLeftOnDevice) {
        BackupError.LocalStorage()
    } else {
        BackupError.Other(retryable)
    }
}

private const val ACCESS_MEDIA_LOCATION = "ACCESS_MEDIA_LOCATION"
