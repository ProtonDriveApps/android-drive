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
import me.proton.core.drive.files.domain.usecase.ChangeParent
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.usecase.DeleteAlbum
import javax.inject.Inject

class SaveChildrenAndDeleteAlbum @Inject constructor(
    private val getPhotosDriveLink: GetPhotosDriveLink,
    private val deleteAlbum: DeleteAlbum,
    private val changeParent: ChangeParent,
) {
    suspend operator fun invoke(
        albumId: AlbumId,
        children: List<FileId>,
    ) = coRunCatching {
        val photosRoot = getPhotosDriveLink(albumId.userId).toResult().getOrThrow()
        val photosRootId = photosRoot.id
        children.forEach { photoId ->
            changeParent(photoId, photosRootId).getOrThrow()
        }
        deleteAlbum(
            volumeId = photosRoot.volumeId,
            albumId = albumId,
        ).getOrThrow()
    }
}
