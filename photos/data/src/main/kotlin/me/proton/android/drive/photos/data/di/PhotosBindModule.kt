/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.photos.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.drive.photos.data.repository.AlbumInfoRepositoryImpl
import me.proton.android.drive.photos.data.repository.MediaStoreVersionRepositoryImpl
import me.proton.android.drive.photos.data.usecase.EnablePhotosBackupImpl
import me.proton.android.drive.photos.data.usecase.GetPagedAddToAlbumPhotoListingsImpl
import me.proton.android.drive.photos.data.usecase.TagsMigrationPrepareFileImpl
import me.proton.android.drive.photos.domain.repository.AlbumInfoRepository
import me.proton.android.drive.photos.domain.repository.MediaStoreVersionRepository
import me.proton.android.drive.photos.domain.usecase.EnablePhotosBackup
import me.proton.android.drive.photos.domain.usecase.FetchAllPhotoListingsImpl
import me.proton.android.drive.photos.domain.usecase.GetPagedAddToAlbumPhotoListings
import me.proton.core.drive.photo.domain.usecase.FetchAllPhotoListings
import me.proton.core.drive.photo.domain.usecase.TagsMigrationPrepareFile
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface PhotosBindModule {

    @Binds
    @Singleton
    fun bindsEnablePhotosBackup(impl: EnablePhotosBackupImpl): EnablePhotosBackup

    @Binds
    @Singleton
    fun bindsMediaStoreVersionRepository(impl: MediaStoreVersionRepositoryImpl): MediaStoreVersionRepository

    @Binds
    @Singleton
    fun bindsAlbumInfoRepository(impl: AlbumInfoRepositoryImpl): AlbumInfoRepository

    @Binds
    @Singleton
    fun bindsGetPagedAddToAlbumPhotoListings(impl: GetPagedAddToAlbumPhotoListingsImpl): GetPagedAddToAlbumPhotoListings

    @Binds
    @Singleton
    fun bindsTagsMigrationPrepareFile(impl: TagsMigrationPrepareFileImpl): TagsMigrationPrepareFile

    @Binds
    @Singleton
    fun bindsFetchAllPhotoListings(impl: FetchAllPhotoListingsImpl): FetchAllPhotoListings
}
