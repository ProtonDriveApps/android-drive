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

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.proton.core.drive.base.data.db.paging.asPagingSource
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.photo.domain.entity.AlbumPhotoListingsPage
import me.proton.core.drive.drivelink.photo.domain.paging.AlbumPhotoListingRemoteMediatorFactory
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class GetPagedAlbumPhotoListings @Inject constructor(
    private val configurationProvider: ConfigurationProvider,
    private val factory: AlbumPhotoListingRemoteMediatorFactory,
) {

    @OptIn(ExperimentalPagingApi::class)
    operator fun invoke(
        pagedListKey: String,
        volumeId: VolumeId,
        albumId: AlbumId,
        remoteAlbumPhotoListings: suspend (pageKey: String?) -> Result<AlbumPhotoListingsPage>,
        deleteAllLocalAlbumPhotoListings: suspend (AlbumId) -> Result<Unit>,
        localPagedAlbumPhotoListings: (Int, Int) -> Flow<Result<List<PhotoListing.Album>>>,
        localAlbumPhotoListingCount: (AlbumId) -> Flow<Int>,
        pageSize: Int = configurationProvider.uiPageSize,
    ): Flow<PagingData<PhotoListing.Album>> = Pager(
        PagingConfig(
            pageSize = pageSize,
            initialLoadSize = pageSize,
            enablePlaceholders = false,
        ),
        remoteMediator = factory.create(
            pagedListKey = pagedListKey,
            volumeId = volumeId,
            albumId = albumId,
            remoteAlbumPhotoListings = remoteAlbumPhotoListings,
            deleteAllLocalAlbumPhotoListings = deleteAllLocalAlbumPhotoListings,
        ),
        pagingSourceFactory = {
            { fromIndex: Int, count: Int ->
                localPagedAlbumPhotoListings(fromIndex, count)
            }.asPagingSource(
                sourceSize = localAlbumPhotoListingCount(albumId),
                observablePageSize = configurationProvider.dbPageSize,
            )
        },
    ).flow
}
