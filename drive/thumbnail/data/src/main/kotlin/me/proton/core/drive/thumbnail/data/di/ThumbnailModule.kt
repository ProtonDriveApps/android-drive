/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.thumbnail.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import me.proton.core.drive.thumbnail.data.provider.AudioThumbnailProvider
import me.proton.core.drive.thumbnail.data.provider.ImageThumbnailProvider
import me.proton.core.drive.thumbnail.data.provider.PdfThumbnailProvider
import me.proton.core.drive.thumbnail.data.provider.SvgThumbnailProvider
import me.proton.core.drive.thumbnail.data.provider.VideoThumbnailProvider
import me.proton.core.drive.thumbnail.domain.usecase.CreateThumbnail

@Module
@InstallIn(SingletonComponent::class)
interface ThumbnailModule {

    @Binds
    @IntoSet
    fun bindsAudioThumbnailProviderIntoList(provider: AudioThumbnailProvider): CreateThumbnail.Provider

    @Binds
    @IntoSet
    fun bindsImageThumbnailProviderIntoList(provider: ImageThumbnailProvider): CreateThumbnail.Provider

    @Binds
    @IntoSet
    fun bindsPdfThumbnailProviderIntoList(provider: PdfThumbnailProvider): CreateThumbnail.Provider

    @Binds
    @IntoSet
    fun bindsSvgThumbnailProviderIntoList(provider: SvgThumbnailProvider): CreateThumbnail.Provider

    @Binds
    @IntoSet
    fun bindsVideoThumbnailProviderIntoList(provider: VideoThumbnailProvider): CreateThumbnail.Provider
}
