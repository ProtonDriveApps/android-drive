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

package me.proton.core.drive.drivelink.photo.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.drivelink.photo.domain.manager.PhotoShareMigrationManager
import me.proton.core.drive.drivelink.photo.domain.usecase.PhotoShareCleanup
import me.proton.core.drive.photo.domain.repository.PhotoShareMigrationRepository
import me.proton.core.drive.volume.domain.usecase.GetVolume
import me.proton.core.drive.volume.domain.usecase.HasPhotoVolume
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DriveLinkPhotoModule {

    @Singleton
    @Provides
    fun providePhotoShareMigrationManager(
        photoShareMigrationRepository: PhotoShareMigrationRepository,
        configurationProvider: ConfigurationProvider,
        getVolume: GetVolume,
        photoShareCleanup: PhotoShareCleanup,
        hasPhotoVolume: HasPhotoVolume,
    ): PhotoShareMigrationManager =
        PhotoShareMigrationManager(
            coroutineContext = Job() + Dispatchers.IO,
            configurationProvider = configurationProvider,
            photoShareMigrationRepository = photoShareMigrationRepository,
            getVolume = getVolume,
            photoShareCleanup = photoShareCleanup,
            hasPhotoVolume = hasPhotoVolume,
        )
}
