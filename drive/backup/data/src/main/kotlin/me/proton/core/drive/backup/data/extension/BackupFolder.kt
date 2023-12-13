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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.db.entity.BackupFolderEntity
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.link.domain.extension.userId

fun BackupFolder.uniqueScanWorkName(userId: UserId) =
    "backup_scan_${userId.id}_${bucketId}"

fun BackupFolder.uniqueUploadWorkName(userId: UserId) =
    "backup_upload_${userId.id}_${bucketId}"

fun BackupFolder.toEntity() = BackupFolderEntity(
    userId = folderId.userId,
    shareId = folderId.shareId.id,
    parentId = folderId.id,
    bucketId = bucketId,
    updateTime = updateTime?.value
)
