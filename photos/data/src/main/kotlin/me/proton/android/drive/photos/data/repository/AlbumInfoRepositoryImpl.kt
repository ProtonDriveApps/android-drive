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

package me.proton.android.drive.photos.data.repository

import androidx.datastore.preferences.core.edit
import me.proton.android.drive.photos.data.db.PhotosDatabase
import me.proton.android.drive.photos.data.db.entity.AddToAlbumEntity
import me.proton.android.drive.photos.data.extension.toAddToAlbumEntity
import me.proton.android.drive.photos.data.extension.toPhotoListing
import me.proton.android.drive.photos.domain.repository.AlbumInfoRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.datastore.GetUserDataStore
import me.proton.core.drive.base.data.extension.get
import me.proton.core.drive.base.domain.function.pagedList
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import javax.inject.Inject

class AlbumInfoRepositoryImpl @Inject constructor(
    private val getUserDataStore: GetUserDataStore,
    private val db: PhotosDatabase,
    private val configurationProvider: ConfigurationProvider,
) : AlbumInfoRepository {

    override suspend fun getName(userId: UserId): String? =
        getUserDataStore(userId).get(GetUserDataStore.Keys.newAlbumName)

    override suspend fun updateName(userId: UserId, name: String) {
        getUserDataStore(userId).edit { preferences ->
            preferences[GetUserDataStore.Keys.newAlbumName] = name
        }
    }

    override suspend fun addPhotoListings(albumId: AlbumId?, vararg photoListings: PhotoListing) =
        db.addToAlbumDao.insertOrIgnore(
            *photoListings.map { photoListing ->
                photoListing.toAddToAlbumEntity(albumId)
            }.toTypedArray()
        )

    override suspend fun removePhotoListings(albumId: AlbumId?, vararg photoListings: PhotoListing) {
        db.inTransaction {
            photoListings
                .groupBy({ photoListing -> photoListing.linkId.shareId }) { photoListing -> photoListing.linkId.id }
                .forEach { (shareId, ids) ->
                    db.addToAlbumDao.delete(shareId.userId, shareId.id, ids.toSet())
                }
        }
    }

    override suspend fun getPhotoListings(userId: UserId, albumId: AlbumId?): List<PhotoListing> {
        val block: suspend (fromIndex: Int, count: Int) -> List<AddToAlbumEntity> =
            albumId?.let {
                { fromIndex, count ->
                    db.addToAlbumDao.getPhotoListings(
                        userId = userId,
                        albumId = albumId.id,
                        offset = fromIndex,
                        limit = count,
                    )
                }
            } ?: let {
                { fromIndex, count ->
                    db.addToAlbumDao.getPhotoListings(
                        userId = userId,
                        offset = fromIndex,
                        limit = count,
                    )
                }
            }
        return pagedList(
            pageSize = configurationProvider.dbPageSize,
        ) { fromIndex, count ->
            block(fromIndex, count)
                .map { addToAlbumEntity -> addToAlbumEntity.toPhotoListing() }
        }
    }

    override suspend fun clear(userId: UserId) {
        getUserDataStore(userId).edit { preferences ->
            preferences.remove(GetUserDataStore.Keys.newAlbumName)
        }
        db.addToAlbumDao.deleteAll(userId)
    }
}
