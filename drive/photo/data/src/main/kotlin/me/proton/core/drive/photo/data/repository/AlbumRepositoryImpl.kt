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

package me.proton.core.drive.photo.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.data.api.PhotoApiDataSource
import me.proton.core.drive.photo.data.db.PhotoDatabase
import me.proton.core.drive.photo.data.db.entity.AlbumListingEntity
import me.proton.core.drive.photo.data.extension.toAlbumListing
import me.proton.core.drive.photo.data.extension.toAlbumListingEntity
import me.proton.core.drive.photo.data.extension.toCreateAlbumRequest
import me.proton.core.drive.photo.data.extension.toUpdateAlbumRequest
import me.proton.core.drive.photo.domain.entity.AlbumInfo
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.photo.domain.entity.UpdateAlbumInfo
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class AlbumRepositoryImpl @Inject constructor(
    private val api: PhotoApiDataSource,
    private val db: PhotoDatabase,
) : AlbumRepository {

    override suspend fun createAlbum(userId: UserId, volumeId: VolumeId, albumInfo: AlbumInfo) =
        api.createAlbum(userId, volumeId, albumInfo.toCreateAlbumRequest()).album.link.linkId

    override suspend fun updateAlbum(
        volumeId: VolumeId,
        albumId: AlbumId,
        updateAlbumInfo: UpdateAlbumInfo,
    ) {
        api.updateAlbum(
            userId = albumId.userId,
            volumeId = volumeId,
            albumId = albumId.id,
            request = updateAlbumInfo.toUpdateAlbumRequest(),
        )
    }

    override suspend fun fetchAndStoreAllAlbumListings(
        userId: UserId,
        volumeId: VolumeId,
        shareId: ShareId,
    ): List<AlbumListing> {
        var anchorId: String? = null
        val albumListingEntities: MutableList<AlbumListingEntity> = mutableListOf()
        while (true) {
            val response = api.getAlbumListings(userId, volumeId, anchorId)
            albumListingEntities.addAll(
                response.albums.map { albumListingsDto ->
                    albumListingsDto.toAlbumListingEntity(shareId)
                }
            )
            if (response.more && response.anchorId != null) {
                anchorId = response.anchorId
            } else {
                break
            }
        }
        db.inTransaction {
            db.albumListingDao.deleteAll(userId, volumeId.id)
            db.albumListingDao.insertOrIgnore(
                *albumListingEntities.toTypedArray()
            )
        }
        return albumListingEntities.map { albumListingEntity -> albumListingEntity.toAlbumListing() }
    }

    override suspend fun deleteAll(userId: UserId, volumeId: VolumeId) =
        db.albumListingDao.deleteAll(userId, volumeId.id)

    override suspend fun getAlbumListings(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction,
    ): List<AlbumListing> =
        db.albumListingDao.getAlbumListings(
            userId = userId,
            volumeId = volumeId.id,
            direction = sortingDirection,
            limit = count,
            offset = fromIndex,
        ).map { albumListingEntity -> albumListingEntity.toAlbumListing() }

    override fun getAlbumListingsFlow(
        userId: UserId,
        volumeId: VolumeId,
        fromIndex: Int,
        count: Int,
        sortingDirection: Direction
    ): Flow<Result<List<AlbumListing>>> =
        db.albumListingDao.getAlbumListingsFlow(
            userId = userId,
            volumeId = volumeId.id,
            direction = sortingDirection,
            limit = count,
            offset = fromIndex,
        ).map { entities ->
            coRunCatching {
                entities.map { albumListingEntity -> albumListingEntity.toAlbumListing() }
            }
        }
}
