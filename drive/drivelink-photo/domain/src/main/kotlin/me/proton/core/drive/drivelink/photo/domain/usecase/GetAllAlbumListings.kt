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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.isOlderThen
import me.proton.core.drive.base.domain.extension.toDataResult
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.BaseRepository
import me.proton.core.drive.base.domain.repository.listFetcherEmitOnEmpty
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.photo.domain.extension.filterBy
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.drive.volume.domain.usecase.GetVolume
import javax.inject.Inject

class GetAllAlbumListings @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val configurationProvider: ConfigurationProvider,
    private val getVolume: GetVolume,
    private val baseRepository: BaseRepository,
) {

    operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        sortingDirection: Direction = Direction.DESCENDING,
        refresh: Flow<Boolean> = flowOf { shouldRefreshAlbumListings(userId, volumeId) },
    ): Flow<DataResult<List<AlbumListing>>> =
        refresh.transform { shouldRefresh ->
            if (shouldRefresh) {
                getVolume(userId, volumeId).toResult().getOrNull("Failed getting volume")
                    ?.let { volume ->
                        listFetcherEmitOnEmpty {
                            albumRepository.fetchAndStoreAllAlbumListings(
                                userId = userId,
                                volumeId = volumeId,
                                shareId = ShareId(userId, volume.shareId),
                            )
                        }
                    }
            }
            emitAll(
                albumRepository.getAlbumListingsFlow(
                    userId = userId,
                    fromIndex = 0,
                    count = configurationProvider.dbPageSize,
                    sortingDirection = sortingDirection,
                ).map { result ->
                    result.toDataResult()
                }
            )
        }

    operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        filterBy: Flow<AlbumListing.Filter>,
        sortingDirection: Direction = Direction.DESCENDING,
        refresh: Flow<Boolean> = flowOf { shouldRefreshAlbumListings(userId, volumeId) },
    ): Flow<DataResult<List<AlbumListing>>> = combine(
        invoke(
            userId = userId,
            volumeId = volumeId,
            sortingDirection = sortingDirection,
            refresh = refresh,
        ),
        filterBy,
    ) { resultAlbumListings, filter ->
        when (resultAlbumListings) {
            is DataResult.Success -> let {
                DataResult.Success(
                    source = ResponseSource.Local,
                    value = resultAlbumListings.value.filterBy(filter, volumeId),
                )
            }
            else -> resultAlbumListings
        }
    }

    private suspend fun shouldRefreshAlbumListings(userId: UserId, volumeId: VolumeId) =
        baseRepository
            .getLastFetch(userId, albumRepository.getAlbumListingsUrl(volumeId))
            .isOlderThen(configurationProvider.minimumAlbumListingFetchInterval)
}
