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
package me.proton.core.drive.files.preview.presentation.component

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.drive.files.preview.R
import me.proton.core.drive.files.preview.presentation.component.state.ZoomEffect

@Composable
@OptIn(ExperimentalCoilApi::class)
fun ImagePreview(
    uri: Uri,
    zoomEffect: Flow<ZoomEffect>,
    isFullScreen: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isFullScreen) {
            MaterialTheme.colors.onBackground
        } else {
            MaterialTheme.colors.background
        }
    )

    val request = ImageRequest.Builder(LocalContext.current)
        .data(uri)
        .memoryCacheKey(uri.path)
        .build()
    val painter = rememberImagePainter(request)
    ImagePreview(
        modifier = modifier.background(backgroundColor),
        painter = painter,
        zoomEffect = zoomEffect,
    )
}

@Composable
fun ImagePreview(
    painter: Painter,
    zoomEffect: Flow<ZoomEffect>,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(LocalContext.current) {
        zoomEffect
            .onEach { zoomEffect ->
                if (zoomEffect is ZoomEffect.Reset) {
                    scale = 1f
                    offset = Offset.Zero
                }
            }
            .launchIn(this)
    }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        if (scale < 1f) scale = 1f
        offset += offsetChange
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(id = R.string.content_description_file_preview),
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(
                    state = state
                )
        )
    }
}

@Preview
@Composable
fun PreviewImagePreview() {
    ProtonTheme {
        ImagePreview(
            uri = Uri.parse("https://protonmail.com/images/media/live/protonmail-shot-decrypt.jpg"),
            zoomEffect = emptyFlow(),
            isFullScreen = false,
        )
    }
}
