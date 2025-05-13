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

package me.proton.core.drive.drivelink.photo.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.photo.domain.entity.PhotoListingsPage
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class FetchPhotoListingPage @Inject constructor(
    private val photoRepository: PhotoRepository,
) {

    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
        pageKey: String?,
        pageSize: Int,
        tag: PhotoTag? = null
    ): Result<PhotoListingsPage> = coRunCatching {
        val (photoListings, saveAction) = photoRepository.fetchPhotoListings(
            userId = userId,
            volumeId = volumeId,
            shareId = shareId,
            pageSize = pageSize,
            previousPageLastLinkId = pageKey,
            tag = tag,
        )
        PhotoListingsPage(
            photoListings = photoListings,
            saveAction = saveAction,
        )
    }
}
