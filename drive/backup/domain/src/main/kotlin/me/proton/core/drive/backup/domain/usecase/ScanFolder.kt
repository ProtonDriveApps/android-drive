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

package me.proton.core.drive.backup.domain.usecase

import me.proton.core.crypto.common.pgp.hmacSha256
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.ScanFolderRepository
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.base.UseHashKey
import javax.inject.Inject

class ScanFolder @Inject constructor(
    private val scanFolder: ScanFolderRepository,
    private val setFiles: SetFiles,
    private val updateFolder: UpdateFolder,
    private val useHashKey: UseHashKey,
) {
    suspend operator fun invoke(
        userId: UserId,
        backupFolder: BackupFolder,
        uploadPriority: Long,
    ) = coRunCatching {
        val files = useHashKey(backupFolder.folderId) { hashKey ->
            scanFolder(backupFolder.bucketId, backupFolder.updateTime).map { backupFile ->
                backupFile.copy(
                    hash = hashKey.hmacSha256(backupFile.name),
                    uploadPriority = uploadPriority,
                )
            }
        }.getOrThrow()
        setFiles(userId, files)
        if (files.isNotEmpty()) {
            updateFolder(userId, backupFolder.bucketId, files.maxOf { media -> media.date })
        }
        files
    }
}