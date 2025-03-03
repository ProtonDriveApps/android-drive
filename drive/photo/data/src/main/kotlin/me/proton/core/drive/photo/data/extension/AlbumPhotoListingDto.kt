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

package me.proton.core.drive.photo.data.extension

import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.data.api.entity.AlbumPhotoListingDto
import me.proton.core.drive.photo.data.db.entity.AlbumPhotoListingEntity
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun AlbumPhotoListingDto.toAlbumPhotoListing(shareId: ShareId, albumId: AlbumId) =
    PhotoListing.Album(
        linkId = FileId(shareId, linkId),
        albumId = albumId,
        captureTime = TimestampS(captureTime),
        nameHash = hash,
        contentHash = contentHash,
        addedTime = TimestampS(addedTime),
        isChildOfAlbum = isChildOfAlbum,
    )

fun AlbumPhotoListingDto.toAlbumPhotoListingEntity(
    volumeId: VolumeId,
    shareId: ShareId,
    albumId: AlbumId,
) =
    AlbumPhotoListingEntity(
        userId = shareId.userId,
        volumeId = volumeId.id,
        shareId = shareId.id,
        albumId = albumId.id,
        linkId = linkId,
        captureTime = captureTime,
        addedTime = addedTime,
        isChildOfAlbum = isChildOfAlbum,
        hash = hash,
        contentHash = contentHash,

    )
