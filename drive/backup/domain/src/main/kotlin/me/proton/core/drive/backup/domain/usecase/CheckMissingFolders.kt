/*
 * Copyright (c) 2025 Proton AG.
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

import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import javax.inject.Inject

class CheckMissingFolders @Inject constructor(
    private val getAllBuckets: GetAllBuckets,
    private val getAllFolders: GetAllFolders,
    private val disableBackupForFolder: DisableBackupForFolder,
) {
    suspend operator fun invoke(userId: UserId) = coRunCatching {
        checkFolders(getAllFolders(userId).getOrThrow())
    }
    suspend operator fun invoke(folderId: FolderId) = coRunCatching {
        checkFolders(getAllFolders(folderId).getOrThrow())
    }

    private suspend fun checkFolders(folders: List<BackupFolder>) {
        val buckets = getAllBuckets().first()
        if (buckets != null) {
            val bucketIds = buckets.map { bucket -> bucket.bucketId }
            val missingFolders = folders.filterNot { folder ->
                folder.bucketId in bucketIds
            }
            missingFolders.forEach { missingFolder ->
                disableBackupForFolder(missingFolder).getOrThrow()
            }
        }
    }
}
