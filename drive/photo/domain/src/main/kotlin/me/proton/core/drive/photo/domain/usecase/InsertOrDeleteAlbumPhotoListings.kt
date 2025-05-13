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
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class InsertOrDeleteAlbumPhotoListings @Inject constructor(
    private val insertOrIgnoreAlbumPhotoListings: InsertOrIgnoreAlbumPhotoListings,
    private val deleteAlbumPhotoListings: DeleteAlbumPhotoListings,
){

    suspend operator fun invoke(volumeId: VolumeId, links: List<Link.File>): Result<Unit> = coRunCatching {
        links
            .filterRelatedPhotos()
            .map { link -> link.toPhotoListing(links) }
            .insertOrIgnorePhotoListings(volumeId)
            .deletePhotoListings(volumeId)
    }

    private suspend fun List<Pair<List<PhotoListing.Album>, Link.State>>.insertOrIgnorePhotoListings(
        volumeId: VolumeId,
    ): List<Pair<List<PhotoListing.Album>, Link.State>> = this.also { photoListingsWithState ->
        photoListingsWithState
            .filter { (_, state) -> state == Link.State.ACTIVE }
            .map { (photoListing, _) -> photoListing }
            .let { photoListings ->
                insertOrIgnoreAlbumPhotoListings(volumeId, photoListings.flatten())
            }
    }

    private suspend fun List<Pair<List<PhotoListing.Album>, Link.State>>.deletePhotoListings(
        volumeId: VolumeId,
    ): List<Pair<List<PhotoListing.Album>, Link.State>> = this.also { photoListingsWithState ->
        photoListingsWithState
            .filter { (_, state) -> state != Link.State.ACTIVE }
            .map { (photoListing, _) -> photoListing }
            .flatten()
            .groupBy { it.albumId }
            .onEach { (albumId, photoListings) ->
                deleteAlbumPhotoListings(albumId.userId, volumeId, albumId, photoListings.map { it.linkId }.toSet())
            }
    }

    private fun List<Link.File>.filterRelatedPhotos() =
        this.let { links ->
            val relatedPhotos = links
                .map { link ->
                    link.relatedPhotoIds
                }
                .flatten()
                .toSet()
            links.filter { link -> link.id !in relatedPhotos }
        }

    private fun Link.File.toPhotoListing(links: List<Link.File>): Pair<List<PhotoListing.Album>, Link.State> {
        val relatedPhotos = relatedPhotoIds.mapNotNull { relatedPhotoId ->
            links.firstOrNull { link -> link.id == relatedPhotoId }
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
        return albumsInfos.map { info ->
            PhotoListing.Album(
                linkId = id,
                captureTime = requireNotNull(photoCaptureTime),
                nameHash = info.hash,
                contentHash = info.contentHash,
                albumId = info.albumId,
                addedTime = info.addedTime,
                isChildOfAlbum = true,
                relatedPhotos = relatedPhotos,
            )
        } to state
    }
}
