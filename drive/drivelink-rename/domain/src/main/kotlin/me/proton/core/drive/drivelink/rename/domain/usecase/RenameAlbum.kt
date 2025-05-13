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

package me.proton.core.drive.drivelink.rename.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.link.CreateRenameInfo
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.drivelink.rename.domain.extension.toUpdateAlbumInfo
import me.proton.core.drive.link.domain.usecase.ValidateLinkNameSize
import javax.inject.Inject

class RenameAlbum @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val updateEventAction: UpdateEventAction,
    private val createRenameInfo: CreateRenameInfo,
    private val getPhotoShare: GetPhotoShare,
    private val getLink: GetLink,
    private val validateLinkNameSize: ValidateLinkNameSize,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        albumId: AlbumId,
        newName: String,
    ) = coRunCatching {

        val photoShare = getPhotoShare(albumId.userId).toResult().getOrThrow()
        val photoShareRoot = getLink(photoShare.rootFolderId).toResult().getOrThrow()
        val album = getLink(albumId).toResult().getOrThrow()
        val renameInfo = createRenameInfo(
            parentFolder = photoShareRoot,
            link = album,
            name = newName,
            mimeType = album.mimeType,
            nameValidator = { validateLinkNameSize(newName).getOrThrow() },
        ).getOrThrow()
        updateEventAction(albumId.userId, volumeId) {
            albumRepository.updateAlbum(
                volumeId = volumeId,
                albumId = albumId,
                updateAlbumInfo = renameInfo.toUpdateAlbumInfo(),
            )
        }
    }
}
