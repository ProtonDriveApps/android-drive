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
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.PrecomputedTextCompat
import androidx.core.widget.TextViewCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.i18n.R as I18N

@Composable
fun TextPreview(
    uri: Uri,
    maxSize: Bytes,
    modifier: Modifier = Modifier,
    onRenderSucceeded: (Any) -> Unit,
    onRenderFailed: (Throwable, Any) -> Unit,
    onDownload: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var isTooLargeToPreview by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r").use { fd ->
                    if (fd != null && fd.length > maxSize.value) {
                        isTooLargeToPreview = true
                    } else {
                        content = fd?.createInputStream()?.bufferedReader()?.use { it.readText() }.orEmpty()
                    }
                }
            } catch (t: Exception) {
                onRenderFailed(t, uri)
            }
        }
    }
    if (isTooLargeToPreview) {
        TooLargeToPreview(
            fileTypeCategory = FileTypeCategory.Text,
            onDownload = onDownload,
            modifier = modifier,
        )
    } else {
        TextPreview(
            content = content,
            onRenderSucceeded = { onRenderSucceeded(uri) },
            modifier = modifier,
        )
    }
}

@Composable
fun TextPreview(
    content: String,
    onRenderSucceeded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(content) {
        if (content.isNotEmpty()) onRenderSucceeded()
    }
    val textStyle = ProtonTheme.typography.defaultSmallNorm
    val textColor = textStyle.color.takeOrElse { ProtonTheme.colors.textNorm }.toArgb()
    val textSizeSp = textStyle.fontSize.value
    var textView by remember { mutableStateOf<TextView?>(null) }
    var renderState by remember(content) { mutableStateOf(RenderState.Loading) }

    LaunchedEffect(content, textView) {
        val currentTextView = textView ?: return@LaunchedEffect
        if (content.length <= INSTANT_RENDER_MAX_CHARS) {
            currentTextView.text = content
        } else {
            val params = TextViewCompat.getTextMetricsParams(currentTextView)
            val precomputedText = withContext(Dispatchers.Default) {
                PrecomputedTextCompat.create(content, params)
            }
            renderState = RenderState.Preparing
            withFrameNanos {}
            TextViewCompat.setPrecomputedText(currentTextView, precomputedText)
        }
        renderState = RenderState.Ready
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = ProtonDimens.MediumSpacing,
                    vertical = ProtonDimens.SmallSpacing
                ),
            factory = { context ->
                ScrollView(context).apply {
                    addView(
                        TextView(context).apply {
                            setTextIsSelectable(true)
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                        }.also { textView = it }
                    )
                }
            },
            update = { scrollView ->
                val currentTextView = scrollView.getChildAt(0) as TextView
                currentTextView.setTextColor(textColor)
                currentTextView.textSize = textSizeSp
            },
        )
        when (renderState) {
            RenderState.Loading, RenderState.Preparing -> PreviewPlaceholder(
                fileTypeCategory = FileTypeCategory.Text,
                message = stringResource(id = I18N.string.preview_processing_state),
            )
            RenderState.Ready -> Unit
        }
    }
}

private enum class RenderState { Loading, Preparing, Ready }

private const val INSTANT_RENDER_MAX_CHARS = 50_000

@Preview
@Composable
fun PreviewTextPreview() {
    ProtonTheme {
        Surface {
            TextPreview(
                content = "Preview text",
                onRenderSucceeded = {},
            )
        }
    }
}
