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
@file:OptIn(ExperimentalFoundationApi::class)

package me.proton.core.drive.files.preview.presentation.component

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.isNightMode
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.i18n.R as I18N

@Composable
fun ImagePreview(
    source: Any,
    transformationState: TransformationState,
    isFullScreen: Boolean,
    onRenderFailed: (Throwable, Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isFullScreen && isNightMode().not()) {
            MaterialTheme.colors.onBackground
        } else {
            MaterialTheme.colors.background
        }
    )
    val cacheKey = remember(source) {
        when (source) {
            is Uri -> source.path
            is ThumbnailVO -> "${source.revisionId}_${source.thumbnailId.type}"
            else -> error("Unhandled cache key for source type $source")
        }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val painter: Painter
    if (size == IntSize.Zero) {
        painter = EmptyPainter
    } else {
        val sizeResolver: SizeResolver = remember(size) {
            SizeResolver(
                Size(
                    width = size.width * LOADING_IMAGE_FACTOR,
                    height = size.height * LOADING_IMAGE_FACTOR,
                )
            )
        }
        val context = LocalContext.current
        val request = remember(source, cacheKey, sizeResolver) {
            ImageRequest.Builder(context)
                .scale(Scale.FIT)
                .data(source)
                .memoryCacheKey(cacheKey)
                .size(sizeResolver)
                .build()
        }
        painter = rememberAsyncImagePainter(request)

        LaunchedEffect(painter.state) {
            val state = painter.state
            if (state is AsyncImagePainter.State.Error) {
                onRenderFailed(state.result.throwable, source)
            }
        }
    }
    ImagePreview(
        modifier = modifier
            .background(backgroundColor)
            .conditional(source !is Uri) {
                fillMaxSize()
            }
            .onSizeChanged {
                size = it
            },
        painter = painter,
        transformationState = transformationState,
    )
}

private const val LOADING_IMAGE_FACTOR = 2

internal object EmptyPainter : Painter() {
    override val intrinsicSize: androidx.compose.ui.geometry.Size get() = androidx.compose.ui.geometry.Size.Unspecified
    override fun DrawScope.onDraw() {}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreview(
    painter: Painter,
    transformationState: TransformationState,
    modifier: Modifier = Modifier,
) {
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        transformationState.scale = (transformationState.scale * zoomChange)
        transformationState.addOffset( offsetChange * transformationState.scale)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { transformationState.containerLayout = it },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(id = I18N.string.content_description_file_preview),
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
                    state = state,
                    canPan = { transformationState.hasScale() },
                )
                .testTag(ImagePreviewComponentTestTag.image)
        )
    }
}

@Preview
@Composable
fun PreviewImagePreview() {
    ProtonTheme {
        ImagePreview(
            source = Uri.parse("https://protonmail.com/images/media/live/protonmail-shot-decrypt.jpg"),
            transformationState = rememberTransformationState(),
            isFullScreen = false,
            onRenderFailed = { _, _ -> }
        )
    }
}


object ImagePreviewComponentTestTag {
    const val image = "preview image"
}
