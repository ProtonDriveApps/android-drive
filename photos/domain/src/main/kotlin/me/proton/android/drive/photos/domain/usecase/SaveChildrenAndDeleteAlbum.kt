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

import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.link.CreateMoveMultipleInfo
import me.proton.core.drive.documentsprovider.domain.usecase.GetFileIdContentDigestMap
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.photo.domain.usecase.ChangeParent
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.exception.LinksResultException
import me.proton.core.drive.link.domain.extension.onFailure
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.usecase.DeleteAlbum
import me.proton.core.drive.photo.domain.usecase.DeleteAlbumPhotoListings
import javax.inject.Inject

class SaveChildrenAndDeleteAlbum @Inject constructor(
    private val getPhotosDriveLink: GetPhotosDriveLink,
    private val deleteAlbum: DeleteAlbum,
    private val getFileIdContentDigestMap: GetFileIdContentDigestMap,
    private val findAndCheckDuplicates: FindAndCheckDuplicates,
    private val createMoveMultipleInfo: CreateMoveMultipleInfo,
    private val changeParent: ChangeParent,
    private val deleteAlbumPhotoListings: DeleteAlbumPhotoListings,
    private val getDriveLink: GetDriveLink,
) {
    suspend operator fun invoke(
        albumId: AlbumId,
        children: Set<FileId>,
    ) = coRunCatching {
        val album = getDriveLink(albumId).toResult().getOrThrow()
        val photosRoot = getPhotosDriveLink(albumId.userId).toResult().getOrThrow()
        val photosRootId = photosRoot.id

        val moveMultipleInfo = createMoveMultipleInfo(
            newParentId = photosRootId,
            linksContentDigests = getFileIdContentDigestMap(children),
        ).getOrThrow()

        val duplicateFileIds = findAndCheckDuplicates(photosRootId, moveMultipleInfo).getOrThrow()

        changeParent(photosRootId, moveMultipleInfo.copy(
            links = moveMultipleInfo.links.filter { link -> link.linkId !in duplicateFileIds }
        ))
            .onSuccess { linksResult ->
                linksResult
                    .onFailure { failedCount, successCount ->
                        throw LinksResultException(failedCount, successCount)
                    }
            }
            .getOrThrow()
        deleteAlbumPhotoListings(
            userId = album.userId,
            volumeId = album.volumeId,
            albumId = album.id,
            fileIds = children,
        ).getOrNull(LogTag.ALBUM, "Failed to delete album photo listings")
        deleteAlbum(
            volumeId = photosRoot.volumeId,
            albumId = albumId,
            deleteAlbumPhotos = duplicateFileIds.isNotEmpty(),
        ).getOrThrow()
    }
}
