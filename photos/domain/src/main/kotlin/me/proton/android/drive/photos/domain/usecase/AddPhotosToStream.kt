/*
 * Copyright (c) 2025 Proton AG.
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

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult
import me.proton.core.drive.photo.domain.usecase.GetAllAlbumChildren
import me.proton.core.drive.photo.domain.usecase.GetAllAlbumDirectChildren
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class AddPhotosToStream @Inject constructor(
    private val getPhotoShare: GetPhotoShare,
    private val getAllAlbumChildren: GetAllAlbumChildren,
    private val addPhotosToFolder: AddPhotosToFolder
) {

    suspend operator fun invoke(
        albumId: AlbumId,
    ): Result<AddToRemoveFromAlbumResult> = coRunCatching {
        val photoIds = getAllAlbumChildren(
            albumId = albumId,
            refresh = true,
            onlyDirectChildren = false,
        ).getOrThrow()
        invoke(albumId, photoIds).getOrThrow()
    }

    suspend operator fun invoke(
        albumId: AlbumId,
        photoIds: List<FileId>,
    ): Result<AddToRemoveFromAlbumResult> = coRunCatching {
        photoIds.takeIfNotEmpty()?.let {
            val userId = photoIds.first().userId
            val photoShare = getPhotoShare(userId).toResult().getOrThrow()
            addPhotosToFolder(
                photoIds = photoIds,
                newVolumeId = photoShare.volumeId,
                folderId = photoShare.rootFolderId,
                albumId = albumId,
            ).getOrThrow()
        } ?: AddToRemoveFromAlbumResult()
    }
}
