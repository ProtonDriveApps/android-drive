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

package me.proton.core.drive.photo.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class InsertOrDeletePhotoListings @Inject constructor(
    private val insertOrIgnorePhotoListings: InsertOrIgnorePhotoListings,
    private val deletePhotoListings: DeletePhotoListings,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        links: List<Link.File>,
    ): Result<Unit> = coRunCatching {
        links
            .map { link -> link.toPhotoListing(links) to link.state }
            .insertOrIgnorePhotoListings(volumeId)
            .deletePhotoListings()
    }

    private suspend fun List<Pair<List<PhotoListing>, Link.State>>.insertOrIgnorePhotoListings(
        volumeId: VolumeId,
    ): List<Pair<List<PhotoListing>, Link.State>> = this.also { photoListingsWithState ->
        photoListingsWithState
            .filter { (_, state) -> state == Link.State.ACTIVE }
            .map { (photoListing, _) -> photoListing }
            .flatten()
            .let { photoListings ->
                insertOrIgnorePhotoListings(volumeId, photoListings)
            }
    }

    private suspend fun List<Pair<List<PhotoListing>, Link.State>>.deletePhotoListings(

    ): List<Pair<List<PhotoListing>, Link.State>> = this.also { photoListingsWithState ->
        photoListingsWithState
            .filter { (_, state) -> state != Link.State.ACTIVE }
            .map { (photoListing, _) -> photoListing }
            .flatten()
            .map { photoListing -> photoListing.linkId }
            .let { linkIds ->
                deletePhotoListings(linkIds)
            }
    }

    private fun Link.File.toPhotoListing(links: List<Link.File>): List<PhotoListing> {
        val relatedPhotos = relatedPhotoIds.mapNotNull { relatedPhotoId ->
            links.firstOrNull { link -> link.id == id }
        }.mapNotNull { link ->
            link.photoCaptureTime?.let { captureTime ->
                PhotoListing.RelatedPhoto(
                    linkId = link.id,
                    captureTime = captureTime,
                    nameHash = link.hash,
                    contentHash = link.photoContentHash
                )
            }
        }
        return photoCaptureTime?.let { captureTime ->
            (tags).map { tag ->
                PhotoListing.Volume(
                    linkId = id,
                    captureTime = captureTime,
                    nameHash = hash,
                    contentHash = photoContentHash,
                    tag = tag,
                    relatedPhotos = relatedPhotos,
                )
            } + PhotoListing.Volume(
                linkId = id,
                captureTime = captureTime,
                nameHash = hash,
                contentHash = photoContentHash,
                tag = null,
                relatedPhotos = relatedPhotos,
            )
        }.orEmpty()
    }
}
