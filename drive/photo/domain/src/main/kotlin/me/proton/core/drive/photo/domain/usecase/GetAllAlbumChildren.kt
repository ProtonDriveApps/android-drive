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

package me.proton.core.drive.photo.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.function.pagedList
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class GetAllAlbumChildren @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val getShare: GetShare,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(
        albumId: AlbumId,
        refresh: Boolean = false,
        onlyDirectChildren: Boolean = false,
    ) = coRunCatching {
        invoke(
            volumeId = getShare(albumId.shareId).toResult().getOrThrow().volumeId,
            albumId = albumId,
            refresh = refresh,
            onlyDirectChildren = onlyDirectChildren,
        ).getOrThrow()
    }

    suspend operator fun invoke(
        volumeId: VolumeId,
        albumId: AlbumId,
        refresh: Boolean = false,
        onlyDirectChildren: Boolean = false,
    ) = coRunCatching {
        if (refresh) {
            var anchorId: String? = null
            var hasMore = true
            while (hasMore) {
                val (listings, saveAction) = albumRepository.fetchAlbumPhotoListings(
                    userId = albumId.userId,
                    volumeId = volumeId,
                    albumId = albumId,
                    anchorId = anchorId,
                    sortingBy = PhotoListing.Album.SortBy.CAPTURED,
                    onlyDirectChildren = onlyDirectChildren,
                )
                anchorId = listings.anchorId
                hasMore = listings.hasMore
                saveAction()
            }
        }
        pagedList(configurationProvider.dbPageSize) { fromIndex, count ->
            albumRepository.getAlbumPhotoListings(
                userId = albumId.userId,
                volumeId = volumeId,
                albumId = albumId,
                fromIndex = fromIndex,
                count = count,
                sortingBy = PhotoListing.Album.SortBy.CAPTURED,
            ).filter { albumPhotoListing ->
                if (onlyDirectChildren) {
                    albumPhotoListing.isChildOfAlbum
                } else {
                    true
                }
            }
        }.map { albumPhotoListing -> albumPhotoListing.linkId }
    }
}
