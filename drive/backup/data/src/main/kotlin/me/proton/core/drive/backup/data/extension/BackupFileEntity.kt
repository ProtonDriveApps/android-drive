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

package me.proton.core.drive.backup.data.extension

import me.proton.core.drive.backup.data.db.entity.BackupFileEntity
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId


fun BackupFileEntity.toBackupFile() = BackupFile(
    bucketId = bucketId,
    folderId = FolderId(
        shareId = ShareId(userId, shareId),
        id = parentId,
    ),
    uriString = uriString,
    mimeType = mimeType,
    name = name,
    hash = hash,
    size = size.bytes,
    state = state,
    date = TimestampS(createTime),
    uploadPriority = uploadPriority,
    attempts = attempts,
    lastModified = lastModified?.let(::TimestampS),
)
