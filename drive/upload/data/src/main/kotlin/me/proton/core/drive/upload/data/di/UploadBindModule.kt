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
package me.proton.core.drive.upload.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import me.proton.core.drive.upload.data.handler.ObservabilityUploadErrorHandler
import me.proton.core.drive.upload.data.manager.UploadErrorManagerImpl
import me.proton.core.drive.upload.data.provider.FileProviderImpl
import me.proton.core.drive.upload.data.resolver.AggregatedUriResolver
import me.proton.core.drive.upload.data.usecase.UploadMetricsNotifierImpl
import me.proton.core.drive.upload.domain.handler.UploadErrorHandler
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.drive.upload.domain.provider.FileProvider
import me.proton.core.drive.upload.domain.resolver.UriResolver
import me.proton.core.drive.upload.domain.usecase.UploadMetricsNotifier
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface UploadBindModule {

    @Binds
    @Singleton
    fun bindsFileProviderImpl(impl: FileProviderImpl): FileProvider

    @Binds
    @Singleton
    fun bindsUriResolver(impl: AggregatedUriResolver): UriResolver

    @Binds
    @Singleton
    fun bindsUploadErrorManagerImpl(impl: UploadErrorManagerImpl): UploadErrorManager

    @Binds
    @IntoSet
    fun bindUploadErrorHandler(impl: ObservabilityUploadErrorHandler): UploadErrorHandler

    @Binds
    @Singleton
    fun bindsUploadMetricsNotifierImpl(impl: UploadMetricsNotifierImpl): UploadMetricsNotifier
}
