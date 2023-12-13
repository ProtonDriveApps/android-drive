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

package me.proton.android.drive.telemetry

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.entity.Event.Backup.BackupState
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.telemetry.domain.event.PhotosEvent.Reason
import javax.inject.Inject

class BackupStoppedEventMapper @Inject constructor(
    private val getPhotoShare: GetPhotoShare,
    private val createPhotosEventBackupStopped: CreatePhotosEventBackupStopped,
) {
    suspend operator fun invoke(userId: UserId, event: Event.BackupStopped) = when (event.state) {
        BackupState.FAILED -> Reason.FAILED_OTHER
        BackupState.FAILED_CONNECTIVITY -> Reason.PAUSED_CONNECTIVITY
        BackupState.FAILED_PERMISSION -> Reason.FAILED_PERMISSIONS
        BackupState.FAILED_LOCAL_STORAGE -> Reason.FAILED_LOCAL_STORAGE
        BackupState.FAILED_DRIVE_STORAGE -> Reason.FAILED_DRIVE_STORAGE
        BackupState.FAILED_PHOTOS_UPLOAD_NOT_ALLOWED -> Reason.FAILED_NOT_ALLOWED
        BackupState.PAUSED_DISABLED -> Reason.PAUSED_DISABLED
        else -> null
    }?.let { reason ->
        getPhotoShare(userId).toResult().getOrNull()?.let { share ->
            createPhotosEventBackupStopped(
                folderId = share.rootFolderId,
                reason = reason,
            )
        }
    }

}
