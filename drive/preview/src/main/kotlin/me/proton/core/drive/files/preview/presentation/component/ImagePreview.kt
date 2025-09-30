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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import me.proton.core.compose.theme.ProtonDimens.DefaultIconSize
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.isNightMode
import me.proton.core.drive.base.presentation.component.Deferred
import me.proton.core.drive.base.presentation.extension.conditional
import me.proton.core.drive.thumbnail.presentation.entity.ThumbnailVO
import me.proton.core.drive.thumbnail.presentation.extension.cacheKey
import me.proton.core.drive.i18n.R as I18N

@Composable
fun ImagePreviewWithThumbnail(
    source: Any?,
    thumbnailSource: Any?,
    transformationState: TransformationState,
    isFullScreen: Boolean,
    onRenderSucceeded: (Any) -> Unit,
    onRenderFailed: (Throwable, Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (thumbnailSource != null) {
            ImagePreview(
                source = thumbnailSource,
                transformationState = transformationState,
                isFullScreen = isFullScreen,
                onRenderSucceeded = onRenderSucceeded,
                onRenderFailed = onRenderFailed,
                modifier = modifier,
            )
            Deferred {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(all = DefaultSpacing)
                        .size(DefaultIconSize)
                        .align(Alignment.BottomStart),
                    strokeWidth = 1.dp,
                )
            }
        }
        if (source is Uri) {
            ImagePreview(
                source = source,
                transformationState = transformationState,
                isFullScreen = isFullScreen,
                onRenderSucceeded = onRenderSucceeded,
                onRenderFailed = onRenderFailed,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun ImagePreview(
    source: Any,
    transformationState: TransformationState,
    isFullScreen: Boolean,
    onRenderSucceeded: (Any) -> Unit,
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
            is ThumbnailVO -> source.cacheKey
            else -> error("Unhandled cache key for source type $source")
        }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var isSourceReady by remember { mutableStateOf(false) }
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
                .listener(onSuccess = { _, _ -> isSourceReady = true })
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
            .conditional(isSourceReady) {
                alpha(1f)
            }
            .conditional(!isSourceReady) {
                alpha(0f)
            }
            .background(backgroundColor)
            .fillMaxSize()
            .onSizeChanged {
                size = it
            },
        painter = painter,
        transformationState = transformationState,
        onAsyncImagePainterSuccess = { onRenderSucceeded(source) },
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
    onAsyncImagePainterSuccess: () -> Unit,
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
                .drawWithContent {
                    drawContent()
                    val asyncImagePainter = painter as? AsyncImagePainter
                    if (asyncImagePainter != null && asyncImagePainter.state is AsyncImagePainter.State.Success) {
                        onAsyncImagePainterSuccess()
                    }
                }
        )
    }
}

@Preview
@Composable
private fun PreviewImagePreview() {
    ProtonTheme {
        ImagePreview(
            source = Uri.parse("https://farm2.staticflickr.com/1533/26541536141_41abe98db3_z_d.jpg"),
            transformationState = rememberTransformationState(),
            isFullScreen = false,
            onRenderSucceeded = { _ -> },
            onRenderFailed = { _, _ -> }
        )
    }
}


object ImagePreviewComponentTestTag {
    const val image = "preview image"
}
