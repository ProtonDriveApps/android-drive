/*
 * Copyright (c) 2023 Proton AG.
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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class FetchAllPhotoListings @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val configurationProvider: ConfigurationProvider,
    private val getPhotoShare: GetPhotoShare,
) {
    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        pageSize: Int = configurationProvider.apiListingPageSize,
    ): Result<Unit> = coRunCatching {
        val share = getPhotoShare(userId, volumeId).toResult().getOrThrow()
        var lastLinkId: String? = null
        do {
            val fetched = photoRepository.fetchAndStorePhotoListings(
                userId = userId,
                volumeId = volumeId,
                shareId = share.id,
                pageSize = pageSize,
                previousPageLastLinkId = lastLinkId,
            )
            lastLinkId = fetched.lastOrNull()?.linkId?.id
        } while (fetched.size == pageSize)
    }
}
