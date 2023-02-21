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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.overline
import me.proton.core.drive.files.preview.presentation.component.state.ZoomEffect

@Composable
fun PdfPreview(
    uri: Uri,
    zoomEffect: Flow<ZoomEffect>,
    modifier: Modifier = Modifier,
    onRenderFailed: (Throwable) -> Unit,
) {
    var pages by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                pages = context.renderPdf(uri)
            } catch (t: Throwable) {
                onRenderFailed(t)
            }
        }
    }
    if (pages.isEmpty()) {
        PdfLoading(modifier)
    } else {
        PdfPreview(
            modifier = modifier,
            zoomEffect = zoomEffect,
            pages = pages,
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
@OptIn(ExperimentalCoilApi::class)
fun PdfPreview(
    zoomEffect: Flow<ZoomEffect>,
    pages: List<ImageBitmap>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        itemsIndexed(pages) { index, item ->
            Column {
                ImagePreview(
                    painter = BitmapPainter(item),
                    modifier = Modifier.fillParentMaxHeight(),
                    zoomEffect = zoomEffect
                )
                PageNumber(
                    pageNumber = index + 1,
                    pageCount = pages.size,
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

@WorkerThread
fun Context.renderPdf(uri: Uri): List<ImageBitmap> {
    val pfd = contentResolver.openAssetFileDescriptor(uri, "r")?.parcelFileDescriptor ?: return emptyList()
    return PdfRenderer(pfd).use { pdfRenderer ->
        (0 until pdfRenderer.pageCount).map { pageIndex ->
            renderPage(
                pdfRenderer = pdfRenderer,
                pageIndex = pageIndex,
            )
        }
    }
}

internal fun renderPage(
    pdfRenderer: PdfRenderer,
    pageIndex: Int,
): ImageBitmap {
    val page = pdfRenderer.openPage(pageIndex) ?: throw IllegalStateException("Open page with index $pageIndex failed.")
    return page.use {
        Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888).also { bitmap ->
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }.asImageBitmap()
    }
}
