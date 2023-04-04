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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.overline
import java.io.IOException
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PdfPreview(
    uri: Uri,
    transformationState: TransformationState,
    modifier: Modifier = Modifier,
    onRenderFailed: (Throwable) -> Unit,
) {
    val context = LocalContext.current

    val pdfReader by produceState<PdfReader?>(initialValue = null, uri) {
        value = PdfReader(context).apply {
            try {
                openPdf(uri)
            } catch (e: IOException){
                onRenderFailed(e)
            }
        }
        awaitDispose {
            launch {
                value?.close()
            }
        }
    }
    val reader = pdfReader
    if (reader == null) {
        PdfLoading(modifier)
    } else {
        PdfPreview(
            modifier = modifier,
            transformationState = transformationState,
            onRenderFailed = onRenderFailed,
            reader = reader,
        )
    }
}

@Composable
fun PdfLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PdfPreview(
    transformationState: TransformationState,
    reader: PdfReader,
    onRenderFailed: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    val density = LocalDensity.current.density
    val maxWidth = with(LocalDensity.current) {
        LocalConfiguration.current.smallestScreenWidthDp.dp.roundToPx() * 2
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { transformationState.containerLayout = it }
            .background(Color.White),
        userScrollEnabled = !transformationState.hasScale(),
        state = lazyListState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState)
    ) {
        items(reader.pageCount) { index ->
            var item by remember { mutableStateOf<ImageBitmap?>(null) }

            LaunchedEffect(index) {
                try {
                    item = reader.renderPage(index, density, maxWidth)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    onRenderFailed(e)
                }
            }

            val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                transformationState.scale = transformationState.scale * zoomChange
                transformationState.addOffset(offsetChange)
            }
            Box(
                modifier = Modifier
                    .fillParentMaxSize()
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                if (item != null) {
                    Image(
                        painter = BitmapPainter(item as ImageBitmap),
                        contentDescription = null,
                        modifier = Modifier
                            .onPlaced { transformationState.contentLayout = it }
                            .graphicsLayer(
                                scaleX = transformationState.scale,
                                scaleY = transformationState.scale,
                                translationX = transformationState.offset.x,
                                translationY = transformationState.offset.y,
                            )
                            .transformable(transformableState),
                    )
                }
                PageNumber(
                    pageNumber = index + 1,
                    pageCount = reader.pageCount,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun PageNumber(
    pageNumber: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .background(ProtonTheme.colors.backgroundSecondary, ProtonTheme.shapes.medium)
                .padding(4.dp),
            text = "$pageNumber / $pageCount",
            style = ProtonTheme.typography.overline
        )
    }
}

class PdfReader(private val context: Context) {
    private var pdfRenderer: PdfRenderer? = null

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: -1

    private val mutex = Mutex()

    @SuppressLint("Recycle")
    suspend fun openPdf(uri: Uri) {
        withContext(Dispatchers.IO) {
            pdfRenderer = context.contentResolver.openAssetFileDescriptor(uri, "r")?.let {
                PdfRenderer(it.parcelFileDescriptor)
            }
        }
    }

    suspend fun close() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                pdfRenderer?.close()
                pdfRenderer = null
            }
        }
    }

    suspend fun renderPage(
        pageIndex: Int,
        density: Float,
        maxWidth: Int,
    ): ImageBitmap {
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                val page = pdfRenderer?.openPage(pageIndex)
                    ?: throw IllegalStateException("Open page with index $pageIndex failed.")
                page.use {
                    val width = min(page.width.fromPtToPx(density), maxWidth)
                    Bitmap.createBitmap(
                        width,
                        page.height * width / page.width,
                        Bitmap.Config.ARGB_8888
                    ).also { bitmap ->
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }.asImageBitmap()
                }
            }
        }
    }

}

private fun Int.fromPtToPx(density: Float): Int =
    (this.toFloat() / 72F * 160F * density).roundToInt()
