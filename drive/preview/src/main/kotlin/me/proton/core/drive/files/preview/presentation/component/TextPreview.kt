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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmall
import java.io.BufferedReader
import java.io.InputStream

@Composable
fun TextPreview(
    uri: Uri,
    modifier: Modifier = Modifier,
    onRenderSucceeded: (Any) -> Unit,
    onRenderFailed: (Throwable, Any) -> Unit,
) {
    var content by remember { mutableStateOf(listOf<String>()) }
    val context = LocalContext.current

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                content =
                    context.contentResolver.openAssetFileDescriptor(uri, "r").use { fd ->
                        fd?.createInputStream()?.readTextLines().orEmpty()
                    }
            } catch (t: Throwable) {
                onRenderFailed(t, uri)
            }
        }
    }
    TextPreview(
        content = content,
        onRenderSucceeded = { onRenderSucceeded(uri) },
        modifier = modifier,
    )
}

internal fun InputStream.readTextLines(): List<String> = mutableListOf<String>().apply {
    BufferedReader(reader()).use { reader ->
        var line = reader.readLine()
        while (line != null) {
            if (line.isEmpty()) {
                add(line)
            } else {
                addAll(line.chunked(MAX_CHARS_BY_LINES))
            }
            line = reader.readLine()
        }
    }
}

@Composable
fun TextPreview(
    content: List<String>,
    onRenderSucceeded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(content) {
        if (content.isNotEmpty()) onRenderSucceeded()
    }
    SelectionContainer {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    horizontal = ProtonDimens.MediumSpacing,
                    vertical = ProtonDimens.SmallSpacing
                )
        ) {
            items(content.size) { i ->
                Text(
                    text = content[i],
                    style = ProtonTheme.typography.defaultSmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewTextPreview() {
    ProtonTheme {
        Surface {
            TextPreview(
                content = listOf("Preview text"),
                onRenderSucceeded = {},
            )
        }
    }
}

// Arbitrary number to prevent crashes from very long lines of text
private const val MAX_CHARS_BY_LINES = 5000
