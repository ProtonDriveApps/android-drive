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

package me.proton.core.drive.drivelink.photo.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.RemoteMediator
import me.proton.core.drive.drivelink.photo.data.db.DriveLinkPhotoDatabase
import me.proton.core.drive.drivelink.photo.domain.entity.AlbumPhotoListingsPage
import me.proton.core.drive.drivelink.photo.domain.paging.AlbumPhotoListingRemoteMediatorFactory
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class AlbumPhotoListingRemoteMediatorFactoryImpl @Inject constructor(
    private val database: DriveLinkPhotoDatabase,
) : AlbumPhotoListingRemoteMediatorFactory {

    @OptIn(ExperimentalPagingApi::class)
    override fun create(
        pagedListKey: String,
        volumeId: VolumeId,
        albumId: AlbumId,
        remoteAlbumPhotoListings: suspend (pageKey: String?) -> Result<AlbumPhotoListingsPage>,
        deleteAllLocalAlbumPhotoListings: suspend (AlbumId) -> Result<Unit>
    ): RemoteMediator<Int, PhotoListing.Album> =
        AlbumPhotoListingRemoteMediator(
            pagedListKey = pagedListKey,
            volumeId =  volumeId,
            albumId = albumId,
            database = database,
            remoteAlbumPhotoListings = remoteAlbumPhotoListings,
            deleteAllLocalAlbumPhotoListings = deleteAllLocalAlbumPhotoListings,
        )
}
