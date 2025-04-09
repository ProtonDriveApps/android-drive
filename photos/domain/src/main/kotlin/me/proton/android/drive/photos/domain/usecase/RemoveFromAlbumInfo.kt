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

import me.proton.android.drive.photos.domain.repository.AlbumInfoRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import javax.inject.Inject

class RemoveFromAlbumInfo @Inject constructor(
    private val repository: AlbumInfoRepository,
) {

    suspend operator fun invoke(
        photoListings: Set<PhotoListing>,
    ) = coRunCatching {
        removeFromAlbumInfo(null, photoListings)
    }

    suspend operator fun invoke(
        albumId: AlbumId,
        photoListings: Set<PhotoListing>,
    ) = coRunCatching {
        removeFromAlbumInfo(albumId, photoListings)
    }

    suspend operator fun invoke(
        userId: UserId,
        albumId: AlbumId?,
    ) = coRunCatching {
        repository.removeAllPhotoListings(userId, albumId)
    }

    private suspend fun removeFromAlbumInfo(albumId: AlbumId?, photoListings: Set<PhotoListing>) =
        repository.removePhotoListings(
            albumId = albumId,
            photoListings = photoListings.toTypedArray(),
        )
}
