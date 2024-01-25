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

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import me.proton.android.drive.photos.domain.handler.DrivePhotosUploadDisabledFeatureFlagHandler
import me.proton.core.drive.backup.domain.usecase.StopBackup
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_PHOTOS_UPLOAD_DISABLED
import me.proton.core.drive.feature.flag.domain.handler.FeatureFlagHandler
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare

@Module
@InstallIn(SingletonComponent::class)
object PhotosModule {

    @Provides
    @IntoMap
    @StringKey(DRIVE_PHOTOS_UPLOAD_DISABLED)
    fun provideFeatureFlagHandler(
        getPhotoShare: GetPhotoShare,
        stopBackup: StopBackup,
    ): FeatureFlagHandler =
        DrivePhotosUploadDisabledFeatureFlagHandler(getPhotoShare, stopBackup)
}
