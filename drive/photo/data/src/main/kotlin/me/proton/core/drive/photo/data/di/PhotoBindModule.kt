/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.photo.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.drive.photo.data.repository.AlbumRepositoryImpl
import me.proton.core.drive.photo.data.repository.PhotoRepositoryImpl
import me.proton.core.drive.photo.data.repository.PhotoShareMigrationRepositoryImpl
import me.proton.core.drive.photo.data.repository.TagRepositoryImpl
import me.proton.core.drive.photo.data.repository.TagsMigrationRepositoryImpl
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.photo.domain.repository.PhotoShareMigrationRepository
import me.proton.core.drive.photo.domain.repository.TagRepository
import me.proton.core.drive.photo.domain.repository.TagsMigrationRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PhotoBindModule {

    @Binds
    @Singleton
    fun bindsPhotoRepositoryImpl(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds
    @Singleton
    fun bindsAlbumRepositoryImpl(impl: AlbumRepositoryImpl): AlbumRepository

    @Binds
    @Singleton
    fun bindsTagRepositoryImpl(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    fun bindsTagsMigrationRepositoryImpl(impl: TagsMigrationRepositoryImpl): TagsMigrationRepository

    @Binds
    @Singleton
    fun bindsPhotoShareMigrationRepositoryImpl(
        impl: PhotoShareMigrationRepositoryImpl,
    ): PhotoShareMigrationRepository
}
