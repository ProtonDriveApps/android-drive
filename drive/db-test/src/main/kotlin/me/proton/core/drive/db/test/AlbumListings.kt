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

package me.proton.core.drive.db.test

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.photo.data.db.entity.AlbumListingEntity
import me.proton.core.drive.photo.data.db.entity.AlbumPhotoListingEntity

suspend fun FolderContext.albumListings(
    vararg albumListings: AlbumListingEntity,
) {
    db.albumListingDao.insertOrUpdate(*albumListings)
}

suspend fun AlbumContext.albumPhotoListings(
    vararg albumPhotoListings: AlbumPhotoListingEntity,
) {
    db.albumPhotoListingDao.insertOrUpdate(*albumPhotoListings)
}

@Suppress("TestFunctionName")
fun AlbumContext.NullableAlbumPhotoListingEntity(
    linkId: String,
    userId: UserId = this.user.userId,
    volumeId: String = this.volume.id,
    shareId: String = this.share.id,
    albumId: String = this.link.id,
    captureTime: Long = 0L,
    addedTime: Long = 0L,
    isChildOfAlbum: Boolean = false,
    hash: String? = null,
    contentHash: String? = null,
) = AlbumPhotoListingEntity(
    userId = userId,
    volumeId = volumeId,
    shareId = shareId,
    albumId = albumId,
    linkId = linkId,
    captureTime = captureTime,
    addedTime = addedTime,
    isChildOfAlbum = isChildOfAlbum,
    hash = hash,
    contentHash = contentHash,
)
