/*
 * Copyright (c) 2025 Proton AG.
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

import kotlinx.coroutines.flow.flowOf
import me.proton.android.drive.photos.domain.repository.AlbumInfoRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.photo.domain.usecase.AddPhotosToAlbum
import me.proton.core.drive.drivelink.photo.domain.usecase.CreateAlbum
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import javax.inject.Inject

class CreateNewAlbum @Inject constructor(
    private val repository: AlbumInfoRepository,
    private val createAlbum: CreateAlbum,
    private val addPhotosToAlbum: AddPhotosToAlbum,
    private val getPhotoShare: GetPhotoShare,
    private val getLink: GetLink,
    private val announceEvent: AsyncAnnounceEvent,
) {
    suspend operator fun invoke(
        userId: UserId,
        isLocked: Boolean = false
    ): Result<AlbumId> = coRunCatching {
        val albumName = requireNotNull(repository.getName(userId)) { "Album name cannot be null" }
        val addToAlbumPhotos = repository.getPhotoListings(userId)
        val albumId = createAlbum(userId, albumName, isLocked).getOrThrow()
        val photoShare = getPhotoShare(userId).toResult().getOrThrow()
        addPhotosToAlbum(
            volumeId = photoShare.volumeId,
            albumId = albumId,
            photoIds = addToAlbumPhotos.map { photoListing ->
                photoListing.linkId
            },
        ).getOrThrow()
        getLink(
            albumId = albumId,
            refresh = flowOf(true)
        ).toResult().getOrNull(LogTag.PHOTO, "Failed to get album link")
        albumId
    }.also { result ->
        result
            .onSuccess { albumId ->
                announceEvent(userId, Event.Album.Created(albumId))
            }
            .onFailure { error ->
                announceEvent(userId, Event.Album.CreationFailed(error))
            }
    }
}
