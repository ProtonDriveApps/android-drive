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

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.proton.core.drive.base.data.db.paging.asPagingSource
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.photo.domain.entity.PhotoListingsPage
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoListingRemoteMediatorFactory
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class GetPagedPhotoListings @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val factory: PhotoListingRemoteMediatorFactory,
) {

    @OptIn(ExperimentalPagingApi::class)
    operator fun invoke(
        volumeId: VolumeId,
        pagedListKey: String,
        tagged: Boolean,
        remotePhotoListings: suspend (pageKey: String?, pageSize: Int) -> Result<PhotoListingsPage>,
        deleteAllLocalPhotoListings: suspend () -> Result<Unit>,
        localPagedPhotoListings: (Int, Int) -> Flow<Result<List<PhotoListing>>>,
        localPhotoListingCount: () -> Flow<Int>,
        pageSize: Int = configurationProvider.uiPageSize,
    ): Flow<PagingData<PhotoListing>> = Pager(
        PagingConfig(
            pageSize = pageSize,
            initialLoadSize = pageSize,
            enablePlaceholders = false,
        ),
        remoteMediator = factory.create(
            volumeId = volumeId,
            pagedListKey = pagedListKey,
            tagged = tagged,
            remotePhotoListings = remotePhotoListings,
            deleteAllLocalPhotoListings = deleteAllLocalPhotoListings,
        ),
        pagingSourceFactory = {
            { fromIndex: Int, count: Int ->
                localPagedPhotoListings(fromIndex, count)
            }.asPagingSource(
                sourceSize = localPhotoListingCount(),
                observablePageSize = maxOf(configurationProvider.dbPageSize, 4 * pageSize),
            )
        },
    ).flow
}
