/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.base.data.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.drive.base.data.provider.AggregatedVideoAttributesProvider
import me.proton.core.drive.base.data.provider.ExifImageAttributesProvider
import me.proton.core.drive.base.data.provider.ExifVideoAttributesProvider
import me.proton.core.drive.base.data.provider.MetadataRetrieverVideoAttributesProvider
import me.proton.core.drive.base.data.provider.MimeTypeProviderImpl
import me.proton.core.drive.base.data.provider.StorageLocationProviderImpl
import me.proton.core.drive.base.data.util.StopWatchImpl
import me.proton.core.drive.base.domain.provider.FileAttributesProvider
import me.proton.core.drive.base.domain.provider.MimeTypeProvider
import me.proton.core.drive.base.domain.provider.StorageLocationProvider
import me.proton.core.drive.base.domain.util.StopWatch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BaseModule {
    @Singleton
    @Provides
    fun provideStorageLocationProvider(
        @ApplicationContext appContext: Context,
    ): StorageLocationProvider =
        StorageLocationProviderImpl(appContext)

    @Singleton
    @Provides
    fun provideMimeTypeProvider(): MimeTypeProvider =
        MimeTypeProviderImpl(Job() + Dispatchers.IO)

    @Singleton
    @Provides
    @IntoSet
    fun provideImageAttributesProvider(
        @ApplicationContext appContext: Context,
    ): FileAttributesProvider =
        ExifImageAttributesProvider(appContext)

    @Singleton
    @Provides
    fun provideExifVideoAttributesProvider(
        @ApplicationContext appContext: Context,
    ): ExifVideoAttributesProvider =
        ExifVideoAttributesProvider(appContext)

    @Singleton
    @Provides
    fun provideMetadataRetrieverVideoAttributesProvider(
        @ApplicationContext appContext: Context,
    ): MetadataRetrieverVideoAttributesProvider =
        MetadataRetrieverVideoAttributesProvider(appContext)

    @Singleton
    @Provides
    @IntoSet
    fun provideVideoAttributesProvider(
        exifVideoAttributesProvider: ExifVideoAttributesProvider,
        metadataRetrieverVideoAttributesProvider: MetadataRetrieverVideoAttributesProvider,
    ): FileAttributesProvider =
        AggregatedVideoAttributesProvider(
            exifVideoAttributesProvider,
            metadataRetrieverVideoAttributesProvider,
        )

    @Provides
    fun provideStopWatch(): StopWatch = StopWatchImpl()
}
