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

package me.proton.android.drive.photos.data.repository

import me.proton.android.drive.photos.data.extension.toBackupDuplicate
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.repository.FindDuplicatesRepository
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import javax.inject.Inject

class PhotoFindDuplicatesRepository @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val getPhotoShare: GetPhotoShare,
) : FindDuplicatesRepository {

    override suspend fun findDuplicates(
        parentId: ParentId,
        nameHashes: List<String>,
        clientUids: List<ClientUid>,
    ): List<BackupDuplicate> {
        val photoShare = getPhotoShare(parentId.userId).toResult().getOrThrow()
        return photoRepository.findDuplicates(
            userId = parentId.userId,
            volumeId = photoShare.volumeId,
            parentId = parentId,
            nameHashes = nameHashes,
            clientUids = emptyList(),
        ).filter { photoDuplicate ->
            photoDuplicate.clientUid.isNullOrEmpty() || photoDuplicate.clientUid in clientUids
        }.map { photoDuplicate -> photoDuplicate.toBackupDuplicate() }
    }
}
