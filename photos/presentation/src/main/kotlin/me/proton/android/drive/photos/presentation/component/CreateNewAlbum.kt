/*
 * Copyright (c) 2025 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.photos.presentation.viewevent.CreateNewAlbumViewEvent
import me.proton.android.drive.photos.presentation.viewstate.CreateNewAlbumViewState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.textNorm
import me.proton.core.drive.i18n.R as I18N

@Composable
fun CreateNewAlbum(
    viewState: CreateNewAlbumViewState,
    viewEvent: CreateNewAlbumViewEvent,
    modifier: Modifier = Modifier,
) {
    val albumName by viewState.name.collectAsStateWithLifecycle(
        initialValue = null
    )
    albumName?.let { name ->
        CreateNewAlbum(
            name = name,
            hint = viewState.hint,
            isEnabled = viewState.isAlbumNameEnabled,
            modifier = modifier,
            onValueChanged = viewEvent.onNameChanged,
        )
    }
}

@Composable
fun CreateNewAlbum(
    name: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    hint: String? = null,
    onValueChanged: (String) -> Unit,
) {
    val state = remember(name) {
        mutableStateOf(
            TextFieldValue(
                text = name,
            )
        )
    }
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        AlbumName(
            textFieldValue = state.value,
            hint = hint,
            isEnabled = isEnabled,
        ) { textField ->
            if (textField.text != state.value.text) {
                onValueChanged(textField.text)
            }
            state.value = textField
        }
    }
}

@Composable
fun AlbumName(
    textFieldValue: TextFieldValue,
    modifier: Modifier = Modifier,
    hint: String? = null,
    maxLines: Int = MaxLines,
    isEnabled: Boolean,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onValueChanged: (TextFieldValue) -> Unit,
) {
    TextField(
        value = textFieldValue,
        placeholder = {
            hint?.let {
                Text(
                    text = hint,
                    style = ProtonTheme.typography.hero,
                    color = ProtonTheme.colors.textHint,
                )
            }
        },
        onValueChange = onValueChanged,
        maxLines = maxLines,
        modifier = modifier
            .focusRequester(focusRequester),
        textStyle = ProtonTheme.typography.hero,
        colors = TextFieldDefaults.protonTextFieldColors(),
        enabled = isEnabled,
    )
}

@Composable
fun TextFieldDefaults.protonTextFieldColors(): TextFieldColors =
    textFieldColors(
        textColor = ProtonTheme.colors.textNorm,
        disabledTextColor = ProtonTheme.colors.textNorm(enabled = false),
        backgroundColor = Color.Transparent,
        focusedLabelColor = Color.Transparent,
        unfocusedLabelColor = Color.Transparent,
        disabledLabelColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        placeholderColor = ProtonTheme.colors.textHint,
        errorLabelColor = ProtonTheme.colors.notificationError,
    )

private const val MaxLines = 4

@Preview
@Composable
private fun EmptyAlbumNamePreview() {
    ProtonTheme {
        AlbumName(
            textFieldValue = TextFieldValue(
                text = "",
            ),
            hint = stringResource(I18N.string.albums_new_album_name_hint),
            isEnabled = true,
            onValueChanged = { _ -> },
        )
    }
}

@Preview
@Composable
private fun MyAlbumNamePreview() {
    ProtonTheme {
        AlbumName(
            textFieldValue = TextFieldValue(
                text = "My album",
            ),
            hint = stringResource(I18N.string.albums_new_album_name_hint),
            isEnabled = true,
            onValueChanged = { _ -> },
        )
    }
}
