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

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

class GetPagedAlbumPhotoListingsList @Inject constructor(
    private val getDriveLink: GetDriveLink,
    private val getPagedAlbumPhotoListings: GetPagedAlbumPhotoListings,
    private val fetchAlbumPhotoListingPage: FetchAlbumPhotoListingPage,
    private val albumRepository: AlbumRepository,
    private val configurationProvider: ConfigurationProvider,
) {

    operator fun invoke(
        albumId: AlbumId,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction,
    ) = getDriveLink(albumId).transform { result ->
        when (result) {
            is DataResult.Processing -> Unit
            is DataResult.Success -> emitAll(
                invoke(result.value, sortingBy, sortingDirection)
            )
            is DataResult.Error -> emit(PagingData.empty())
        }.exhaustive
    }

    operator fun invoke(
        album: DriveLink.Album,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction,
    ): Flow<PagingData<PhotoListing.Album>> =
        getPagedAlbumPhotoListings(
            pagedListKey = "ALBUM_PHOTO_LISTING",
            volumeId = album.volumeId,
            albumId = album.id,
            remoteAlbumPhotoListings = { pageKey ->
                fetchAlbumPhotoListingPage(
                    userId = album.userId,
                    volumeId =  album.volumeId,
                    albumId = album.id,
                    pageKey = pageKey,
                    sortingBy = sortingBy,
                    sortingDirection = sortingDirection,
                )
            },
            deleteAllLocalAlbumPhotoListings = {
                coRunCatching {
                    albumRepository.deleteAllAlbumPhotoListings(
                        userId = album.userId,
                        volumeId = album.volumeId,
                        albumId = album.id,
                    )
                }
            },
            localPagedAlbumPhotoListings = { fromIndex, count ->
                albumRepository.getAlbumPhotoListingsFlow(
                    userId = album.userId,
                    volumeId = album.volumeId,
                    albumId = album.id,
                    fromIndex = fromIndex,
                    count = count,
                    sortingBy = sortingBy,
                    sortingDirection = sortingDirection,
                )
            },
            localAlbumPhotoListingCount = {
                albumRepository.getAlbumPhotoListingCount(
                    userId = album.userId,
                    volumeId = album.volumeId,
                    albumId = album.id,
                )
            },
            pageSize = configurationProvider.uiPageSize,
        )
}
