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

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

class GetPagedPhotoListingsList @Inject constructor(
    private val getPagedPhotoListings: GetPagedPhotoListings,
    private val getPhotoShare: GetPhotoShare,
    private val photoRepository: PhotoRepository,
    private val fetchPhotoListingPage: FetchPhotoListingPage,
    private val configurationProvider: ConfigurationProvider,
) {

    operator fun invoke(userId: UserId, tag: PhotoTag? = null): Flow<PagingData<PhotoListing>> = getPhotoShare(userId)
        .transform { result ->
            when (result) {
                is DataResult.Processing -> Unit
                is DataResult.Success -> emitAll(
                    invoke(result.value, tag)
                )
                is DataResult.Error -> emit(PagingData.empty())
            }.exhaustive
        }

    operator fun invoke(share: Share, tag: PhotoTag? = null): Flow<PagingData<PhotoListing>> =
        getPagedPhotoListings(
            volumeId = share.volumeId,
            pagedListKey = tag?.let { "PHOTO_LISTING_$tag" } ?: "PHOTO_LISTING",
            tagged = tag != null,
            remotePhotoListings = { pageKey, _ ->
                fetchPhotoListingPage(
                    userId = share.id.userId,
                    volumeId =  share.volumeId,
                    shareId = share.id,
                    pageKey = pageKey,
                    pageSize = configurationProvider.apiListingPageSize,
                    tag = tag,
                )
            },
            localPagedPhotoListings = { fromIndex, count ->
                photoRepository.getPhotoListingsFlow(
                    userId = share.id.userId,
                    volumeId = share.volumeId,
                    fromIndex = fromIndex,
                    count = count,
                    tag = tag,
                )
            },
            localPhotoListingCount = { photoRepository.getPhotoListingCount(
                userId = share.id.userId,
                volumeId = share.volumeId,
                tag = tag,
            ) },
            deleteAllLocalPhotoListings = {
                coRunCatching {
                    photoRepository.deleteAll(
                        userId = share.id.userId,
                        volumeId = share.volumeId,
                        tag = tag,
                    )
                }
            }
        )
}
