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

package me.proton.android.drive.photos.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BucketRepository
import me.proton.core.drive.backup.domain.usecase.AddFolder
import me.proton.core.drive.base.domain.extension.firstSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class SetupPhotosBackup @Inject constructor(
    private val getPhotosDriveLink: GetPhotosDriveLink,
    private val addFolder: AddFolder,
    private val bucketRepository: BucketRepository,
) {

    suspend operator fun invoke(
        userId: UserId,
        folderName: String
    ) = coRunCatching {
        val bucketEntries = bucketRepository.getAll()
        val photoRootId = getPhotosDriveLink(userId)
            .firstSuccessOrError().toResult().getOrThrow().id
        bucketEntries.filter { entry ->
            entry.bucketName == folderName
        }.map { entry ->
            BackupFolder(
                bucketId = entry.bucketId,
                folderId = photoRootId,
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
