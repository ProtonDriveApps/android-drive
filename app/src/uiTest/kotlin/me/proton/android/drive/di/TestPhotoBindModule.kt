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

package me.proton.android.drive.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import me.proton.android.drive.repository.TestPhotoShareMigrationRepositoryImpl
import me.proton.core.drive.photo.data.di.PhotoBindModule
import me.proton.core.drive.photo.data.repository.AlbumRepositoryImpl
import me.proton.core.drive.photo.data.repository.PhotoRepositoryImpl
import me.proton.core.drive.photo.data.repository.TagRepositoryImpl
import me.proton.core.drive.photo.data.repository.TagsMigrationRepositoryImpl
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.photo.domain.repository.PhotoRepository
import me.proton.core.drive.photo.domain.repository.PhotoShareMigrationRepository
import me.proton.core.drive.photo.domain.repository.TagRepository
import me.proton.core.drive.photo.domain.repository.TagsMigrationRepository
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PhotoBindModule::class]
)
interface TestPhotoBindModule {

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
        impl: TestPhotoShareMigrationRepositoryImpl,
    ): PhotoShareMigrationRepository
}

