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

package me.proton.android.drive.photos.data.usecase

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.android.drive.photos.data.db.PhotosDatabase
import me.proton.android.drive.photos.data.extension.toPhotoListing
import me.proton.android.drive.photos.domain.usecase.GetPagedAddToAlbumPhotoListings
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import javax.inject.Inject

class GetPagedAddToAlbumPhotoListingsImpl @Inject constructor(
    private val db: PhotosDatabase,
    private val configurationProvider: ConfigurationProvider,
) : GetPagedAddToAlbumPhotoListings {

    override fun invoke(
        userId: UserId,
        albumId: AlbumId?,
    ): Flow<PagingData<PhotoListing>> =
        Pager(
            PagingConfig(
                pageSize = configurationProvider.uiPageSize,
                enablePlaceholders = true,
            ),
            pagingSourceFactory = {
                if (albumId != null) {
                    db.addToAlbumDao.getAddToAlbumPhotosPagingSource(
                        userId = userId,
                        albumId = albumId.id,
                    )
                } else {
                    db.addToAlbumDao.getAddToAlbumPhotosPagingSource(
                        userId = userId,
                    )
                }
            }
        )
            .flow
            .map { pagingData ->
                pagingData.map { addToAlbumEntity -> addToAlbumEntity.toPhotoListing() }
            }
}
