/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.backup.domain.entity

data class BackupError(
    val type: BackupErrorType,
    val retryable: Boolean,
) {
    @Suppress("FunctionName")
    companion object {
        fun Other(retryable: Boolean = true) = BackupError(
            type = BackupErrorType.OTHER,
            retryable = retryable,
        )

        fun LocalStorage() = BackupError(
            type = BackupErrorType.LOCAL_STORAGE,
            retryable = true,
        )

        fun DriveStorage() = BackupError(
            type = BackupErrorType.DRIVE_STORAGE,
            retryable = true,
        )

        fun Permissions() = BackupError(
            type = BackupErrorType.PERMISSION,
            retryable = true,
        )

        fun Connectivity() = BackupError(
            type = BackupErrorType.CONNECTIVITY,
            retryable = true,
        )

        fun WifiConnectivity() = BackupError(
            type = BackupErrorType.WIFI_CONNECTIVITY,
            retryable = true,
        )

        fun PhotosUploadNotAllowed() = BackupError(
            type = BackupErrorType.PHOTOS_UPLOAD_NOT_ALLOWED,
            retryable = true,
        )
    }
}
