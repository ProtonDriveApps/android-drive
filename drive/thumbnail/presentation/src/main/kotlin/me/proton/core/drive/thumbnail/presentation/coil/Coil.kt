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

package me.proton.core.drive.thumbnail.presentation.coil

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.compose.LocalImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.crypto.domain.usecase.DecryptThumbnail
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailFile
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailInputStream
import me.proton.core.drive.thumbnail.presentation.coil.decode.ThumbnailDecoder
import me.proton.core.drive.thumbnail.presentation.coil.fetch.ThumbnailFetcher
import me.proton.core.drive.thumbnail.presentation.coil.fetch.ThumbnailKeyer

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ThumbnailEnabled(
    configurationProvider: ConfigurationProvider,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val currentImageLoader = LocalImageLoader.current
    val imageLoader = remember {
        val injections = EntryPointAccessors.fromApplication(context, HiltEntryPoint::class.java)
        val thumbnailFetcherFactory = ThumbnailFetcher.Factory(
            context = context,
            getThumbnailInputStream = injections.getThumbnailInputStream,
            getThumbnailFile = injections.getThumbnailFile
        )
        val thumbnailDecoderFactory = ThumbnailDecoder.Factory(
            context = context,
            maxThumbnail = configurationProvider.thumbnailPhoto,
            decryptThumbnail = injections.decryptThumbnail
        )
        currentImageLoader.newBuilder()
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
                add(ThumbnailKeyer)
                add(thumbnailFetcherFactory)
                add(thumbnailDecoderFactory)
            }
            .build()
    }
    CompositionLocalProvider(LocalImageLoader provides imageLoader) {
        content()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltEntryPoint {
    val getThumbnailInputStream: GetThumbnailInputStream
    val getThumbnailFile: GetThumbnailFile
    val decryptThumbnail: DecryptThumbnail
}
