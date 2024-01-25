/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.android.drive.photos.domain.usecase

import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BucketRepository
import me.proton.core.drive.backup.domain.usecase.AddFolder
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class SetupPhotosBackup @Inject constructor(
    private val setupPhotosConfigurationBackup: SetupPhotosConfigurationBackup,
    private val addFolder: AddFolder,
    private val bucketRepository: BucketRepository,
) {

    suspend operator fun invoke(
        folderId: FolderId,
        folderName: String,
    ) = coRunCatching {
        setupPhotosConfigurationBackup(folderId).getOrThrow()
        val bucketEntries = bucketRepository.getAll()
        bucketEntries.filter { entry ->
            entry.bucketName == folderName
        }.map { entry ->
            BackupFolder(
                bucketId = entry.bucketId,
                folderId = folderId,
            )
        }.onEach { backupFolder ->
            addFolder(backupFolder).getOrThrow()
        }.also { defaultBucketEntries ->
            if (defaultBucketEntries.isEmpty()) {
                CoreLogger.w(BACKUP, "No bucket with name: $folderName in $bucketEntries")
            } else {
                CoreLogger.d(BACKUP, "Setup photos backup for ${defaultBucketEntries.size} buckets")
            }
        }
    }
}
