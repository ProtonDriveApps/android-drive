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

package me.proton.core.drive.photo.data.extension

import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.data.db.entity.AlbumPhotoListingEntity
import me.proton.core.drive.photo.data.db.entity.AlbumRelatedPhotoEntity
import me.proton.core.drive.photo.data.db.entity.PhotoListingEntity
import me.proton.core.drive.photo.data.db.entity.RelatedPhotoEntity
import me.proton.core.drive.photo.data.db.entity.TaggedPhotoListingEntity
import me.proton.core.drive.photo.data.db.entity.TaggedRelatedPhotoEntity
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.volume.domain.entity.VolumeId

fun PhotoListing.toPhotoListingEntity(volumeId: VolumeId) =
    PhotoListingEntity(
        userId = linkId.userId,
        volumeId = volumeId.id,
        shareId = linkId.shareId.id,
        linkId = linkId.id,
        captureTime = captureTime.value,
        hash = nameHash,
        contentHash = contentHash,
    ) to relatedPhotos.map { relatedPhoto ->
        RelatedPhotoEntity(
            userId = linkId.shareId.userId,
            volumeId = volumeId.id,
            shareId = linkId.shareId.id,
            linkId = relatedPhoto.linkId.id,
            mainPhotoLinkId = linkId.id,
            captureTime = relatedPhoto.captureTime.value,
            hash = relatedPhoto.nameHash,
            contentHash = relatedPhoto.contentHash,
        )
    }

fun PhotoListing.toTaggedPhotoListingEntity(volumeId: VolumeId) =
    TaggedPhotoListingEntity(
        userId = linkId.userId,
        volumeId = volumeId.id,
        shareId = linkId.shareId.id,
        tag = requireNotNull(tag?.value) { "tag is required to create TaggedPhotoListingEntity"},
        linkId = linkId.id,
        captureTime = captureTime.value,
        hash = nameHash,
        contentHash = contentHash,
    ) to relatedPhotos.map { relatedPhoto ->
        TaggedRelatedPhotoEntity(
            userId = linkId.shareId.userId,
            volumeId = volumeId.id,
            shareId = linkId.shareId.id,
            tag = requireNotNull(tag?.value) { "tag is required to create TaggedPhotoListingEntity"},
            linkId = relatedPhoto.linkId.id,
            mainPhotoLinkId = linkId.id,
            captureTime = relatedPhoto.captureTime.value,
            hash = relatedPhoto.nameHash,
            contentHash = relatedPhoto.contentHash,
        )
    }

fun PhotoListing.Album.toAlbumPhotoListingEntity(volumeId: VolumeId) =
    AlbumPhotoListingEntity(
        userId = linkId.userId,
        volumeId = volumeId.id,
        shareId = linkId.shareId.id,
        albumId = albumId.id,
        linkId = linkId.id,
        captureTime = captureTime.value,
        addedTime = addedTime.value,
        isChildOfAlbum = isChildOfAlbum,
        hash = nameHash,
        contentHash = contentHash,
    ) to relatedPhotos.map { relatedPhoto ->
        AlbumRelatedPhotoEntity(
            userId = linkId.shareId.userId,
            volumeId = volumeId.id,
            shareId = linkId.shareId.id,
            albumId = albumId.id,
            linkId = relatedPhoto.linkId.id,
            mainPhotoLinkId = linkId.id,
            captureTime = relatedPhoto.captureTime.value,
            hash = relatedPhoto.nameHash,
            contentHash = relatedPhoto.contentHash,
        )
    }

fun PhotoListing.Album.SortBy.toDtoSort(): String = when (this) {
    PhotoListing.Album.SortBy.CAPTURED -> "Captured"
    PhotoListing.Album.SortBy.ADDED -> "Added"
}
