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

package me.proton.android.drive.photos.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.photo.domain.entity.PhotoListing

interface AlbumInfoRepository {

    suspend fun getName(userId: UserId): String?
    suspend fun updateName(userId: UserId, name: String)
    suspend fun addPhotoListings(albumId: AlbumId? = null, vararg photoListings: PhotoListing)
    suspend fun removePhotoListings(albumId: AlbumId? = null, vararg photoListings: PhotoListing)
    suspend fun removeAllPhotoListings(userId: UserId, albumId: AlbumId? = null)
    suspend fun getPhotoListings(userId: UserId, albumId: AlbumId? = null): List<PhotoListing>
    suspend fun clear(userId: UserId)
    fun getPhotoListingsCount(userId: UserId, albumId: AlbumId?): Flow<Int>
}
