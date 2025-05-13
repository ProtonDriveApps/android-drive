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
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class InsertOrIgnoreAlbumPhotoListings @Inject constructor(
    private val albumRepository: AlbumRepository,
) {

    suspend operator fun invoke(volumeId: VolumeId, albumPhotoListings: List<PhotoListing.Album>): Result<Unit> = coRunCatching {
        albumRepository.insertOrIgnoreAlbumPhotoListing(volumeId, albumPhotoListings)
    }
}
