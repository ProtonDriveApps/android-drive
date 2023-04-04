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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.isNightMode
import me.proton.core.drive.files.preview.R

@Composable
fun ImagePreview(
    uri: Uri,
    transformationState: TransformationState,
    isFullScreen: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isFullScreen && isNightMode().not()) {
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
        transformationState = transformationState,
    )
}

@Composable
fun ImagePreview(
    painter: Painter,
    transformationState: TransformationState,
    modifier: Modifier = Modifier,
) {
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        transformationState.scale = (transformationState.scale * zoomChange)
        transformationState.addOffset(offsetChange)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { transformationState.containerLayout = it },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(id = R.string.content_description_file_preview),
            contentScale = ContentScale.Fit,
            modifier = modifier
                .onPlaced { transformationState.contentLayout = it }
                .graphicsLayer(
                    scaleX = transformationState.scale,
                    scaleY = transformationState.scale,
                    translationX = transformationState.offset.x,
                    translationY = transformationState.offset.y,
                    clip = true,
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
            transformationState = rememberTransformationState(),
            isFullScreen = false,
        )
    }
}
