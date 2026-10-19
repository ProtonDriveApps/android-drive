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
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.data.db.entity.TaggedPhotoListingWithFileProperties
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun TaggedPhotoListingWithFileProperties.toPhotoListing(
    volumeId: VolumeId = VolumeId(taggedPhotoListingEntity.volumeId),
    shareId: ShareId = ShareId(taggedPhotoListingEntity.userId, taggedPhotoListingEntity.shareId),
) =
    with (taggedPhotoListingEntity) {
        PhotoListing.Volume(
            volumeId = volumeId,
            linkId = FileId(
                shareId = shareId,
                id = linkId,
            ),
            captureTime = TimestampS(captureTime),
            nameHash = hash,
            contentHash = contentHash,
            tag = PhotoTag.fromLong(tag),
            activeRevisionId = activeRevisionId,
            defaultThumbnailId = defaultThumbnailId,
        )
    }

fun List<TaggedPhotoListingWithFileProperties>.toPhotoListings(): List<PhotoListing> {
    val shareIds = mutableMapOf<String, ShareId>()
    val volumeIds = mutableMapOf<String, VolumeId>()
    return map { taggedPhotoListingWithFileProperties ->
        with(taggedPhotoListingWithFileProperties.taggedPhotoListingEntity) {
            taggedPhotoListingWithFileProperties.toPhotoListing(
                volumeId = volumeIds.getOrPut(volumeId) { VolumeId(volumeId) },
                shareId = shareIds.getOrPut(shareId) { ShareId(userId, shareId) },
            )
        }
    }
}
