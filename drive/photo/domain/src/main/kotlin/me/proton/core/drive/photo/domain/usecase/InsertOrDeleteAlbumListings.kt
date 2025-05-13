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

package me.proton.core.drive.photo.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.isSharedByLinkOrWithUsers
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class InsertOrDeleteAlbumListings @Inject constructor(
    private val insertOrUpdateAlbumListings: InsertOrUpdateAlbumListings,
    private val deleteAlbumListings: DeleteAlbumListings,
) {

    suspend operator fun invoke(
        volumeId: VolumeId,
        albums: List<Link.Album>
    ): Result<Unit> = coRunCatching {
        albums
            .map { album -> album.toAlbumListing(volumeId) to album.state }
            .insertOrUpdateAlbumListings()
            .deleteAlbumListings()
    }

    private suspend fun List<Pair<AlbumListing, Link.State>>.insertOrUpdateAlbumListings(
    ): List<Pair<AlbumListing, Link.State>> = this.also { albumListingsWithState ->
        albumListingsWithState
            .filter { (_, state) -> state == Link.State.ACTIVE }
            .map { (albumListing, _) -> albumListing }
            .let { albumListing ->
                insertOrUpdateAlbumListings(albumListing)
            }
    }

    private suspend fun List<Pair<AlbumListing, Link.State>>.deleteAlbumListings(
    ): List<Pair<AlbumListing, Link.State>> = this.also { albumListingsWithState ->
        albumListingsWithState
            .filter { (_, state) -> state != Link.State.ACTIVE }
            .map { (albumListing, _) -> albumListing.albumId }
            .let { albumIds ->
                deleteAlbumListings(albumIds)
            }
    }

    private fun Link.Album.toAlbumListing(volumeId: VolumeId): AlbumListing =
        AlbumListing(
            volumeId = volumeId,
            albumId = id,
            isLocked = isLocked,
            photoCount = photoCount,
            lastActivityTime = lastActivityTime,
            coverLinkId = coverLinkId,
            isShared = isSharedByLinkOrWithUsers,
        )
}
