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

package me.proton.core.drive.photo.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.SaveAction
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.photo.domain.entity.AddToAlbumInfo
import me.proton.core.drive.photo.domain.entity.AlbumInfo
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.photo.domain.entity.AlbumPhotoListingList
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.photo.domain.entity.UpdateAlbumInfo
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId

interface AlbumRepository {

    suspend fun createAlbum(userId: UserId, volumeId: VolumeId, albumInfo: AlbumInfo): String

    suspend fun updateAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        updateAlbumInfo: UpdateAlbumInfo,
    )

    suspend fun fetchAndStoreAllAlbumListings(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
    ): List<AlbumListing>

    suspend fun deleteAll(userId: UserId, volumeId: VolumeId)

    suspend fun getAlbumListings(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction = Direction.DESCENDING,
    ): List<AlbumListing>

    fun getAlbumListingsFlow(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction = Direction.DESCENDING,
    ): Flow<Result<List<AlbumListing>>>

    suspend fun insertOrIgnoreAlbumListings(
        albumListings: List<AlbumListing>,
    )

    suspend fun deleteAlbumListings(albumIds: List<AlbumId>)

    suspend fun fetchAlbumPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
        anchorId: String?,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction = Direction.DESCENDING,
    ): Pair<AlbumPhotoListingList, SaveAction>

    suspend fun fetchAndStoreAlbumPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
        albumId: AlbumId,
        anchorId: String?,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction = Direction.DESCENDING,
    ): List<PhotoListing.Album>

    suspend fun getAlbumPhotoListings(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
        fromIndex: Int,
        count: Int,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction = Direction.DESCENDING,
    ): List<PhotoListing.Album>

    fun getAlbumPhotoListingsFlow(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
        fromIndex: Int,
        count: Int,
        sortingBy: PhotoListing.Album.SortBy,
        sortingDirection: Direction = Direction.DESCENDING,
    ): Flow<Result<List<PhotoListing.Album>>>

    fun getAlbumPhotoListingCount(
        userId: UserId,
        volumeId: VolumeId,
        albumId: AlbumId,
    ): Flow<Int>

    suspend fun deleteAllAlbumPhotoListings(userId: UserId, volumeId: VolumeId, albumId: AlbumId)

    suspend fun insertOrIgnoreAlbumPhotoListing(
        volumeId: VolumeId,
        photoListings: List<PhotoListing.Album>,
    )

    fun getAlbumListingsUrl(volumeId: VolumeId) = "drive/photos/volumes/${volumeId.id}/albums"

    suspend fun addToAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        addToAlbumInfos: List<AddToAlbumInfo>,
    )

    suspend fun deleteAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        deleteAlbumPhotos: Boolean,
    )
}
