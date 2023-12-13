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

package me.proton.core.drive.backup.data.worker

import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink

@Suppress("TestFunctionName")
internal fun NullableBackupFile(
    bucketId: Int,
    uriString: String = "uri",
    name: String = "photo.jpg",
    hash: String? = "hash",
    date: TimestampS = TimestampS(0),
    size: Bytes = 0.bytes,
    state: BackupFileState = BackupFileState.IDLE,
) = BackupFile(
    bucketId = bucketId,
    uriString = uriString,
    mimeType = "image/jpeg",
    name = name,
    hash = hash,
    size = size,
    state = state,
    date = date,
    uploadPriority = UploadFileLink.BACKUP_PRIORITY,
)
