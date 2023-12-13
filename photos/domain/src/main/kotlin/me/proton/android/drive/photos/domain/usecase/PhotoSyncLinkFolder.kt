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
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.stats.domain.usecase.GetUploadStats
import javax.inject.Inject

class PhotoSyncLinkFolder @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val configurationProvider: ConfigurationProvider,
    private val getPhotoShare: GetPhotoShare,
    private val getUploadStats: GetUploadStats,
) {
    suspend operator fun invoke(
        userId: UserId,
    ) = coRunCatching {
        getPhotoShare(userId).toResult().getOrNull()?.let { share ->
            val pageSize = configurationProvider.apiListingPageSize
            val minimumCaptureTime = getUploadStats(share.rootFolderId)
                .getOrNull()
                ?.minimumFileCreationDateTime ?: TimestampS(0)
            var lastLinkId: String? = null
            var pageMinimumCaptureTime: TimestampS
            do {
                val fetched = photoRepository.fetchAndStorePhotoListings(
                    userId = userId,
                    volumeId = share.volumeId,
                    shareId = share.id,
                    pageSize = pageSize,
                    previousPageLastLinkId = lastLinkId,
                    minimumCaptureTime = minimumCaptureTime
                )
                pageMinimumCaptureTime =
                    fetched.minOf { photoListing -> photoListing.captureTime }
                lastLinkId = fetched.lastOrNull()?.linkId?.id
            } while (fetched.size == pageSize && pageMinimumCaptureTime > minimumCaptureTime)
        }
    }
}
