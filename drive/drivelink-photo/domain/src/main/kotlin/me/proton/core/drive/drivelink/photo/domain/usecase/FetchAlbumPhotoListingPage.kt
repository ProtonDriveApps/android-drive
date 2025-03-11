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

package me.proton.core.drive.drivelink.photo.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.photo.domain.entity.AlbumPhotoListingsPage
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class FetchAlbumPhotoListingPage @Inject constructor(
    private val albumRepository: AlbumRepository,
) {
    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
        pageKey: String?,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction,
    ): Result<AlbumPhotoListingsPage> = coRunCatching {
        val (albumPhotoListings, saveAction) = albumRepository.fetchAlbumPhotoListings(
            userId = userId,
            volumeId = volumeId,
            albumId = albumId,
            anchorId = pageKey,
            sortingBy = sortingBy,
            sortingDirection = sortingDirection,
        )
        AlbumPhotoListingsPage(
            albumPhotoListings = albumPhotoListings.list,
            anchoreId = albumPhotoListings.anchorId,
            hasMore = albumPhotoListings.hasMore,
            saveAction = saveAction,
        )
    }
}
