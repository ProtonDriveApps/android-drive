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
package me.proton.core.drive.base.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.caption
import me.proton.core.compose.theme.default

@Composable
fun OutlinedTextFieldWithError(
    text: String,
    modifier: Modifier = Modifier,
    selection: IntRange = IntRange(text.length, text.length),
    errorText: String? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    maxLines: Int = MaxLines,
    onValueChanged: (String) -> Unit,
) {
    // This code is based on BasicTextField:122
    // Holds the latest internal TextFieldValue state. We need to keep it to have the correct value
    // of the composition.
    var textFieldValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(selection.first, selection.last)
            )
        )
    }
    // Holds the latest TextFieldValue that OutlinedTextFieldWithError was recomposed with.
    // We couldn't simply pass `TextFieldValue(text = text, selection=[..])` to the CoreTextField
    // because we need to preserve the composition.
    val textFieldValue = textFieldValueState.copy(
        text = text,
        selection = TextRange(selection.first, selection.last),
    )
    // Last String value that either text field was recomposed with or updated in the onValueChange
    // callback. We keep track of it to prevent calling onValueChange(String) for same String when
    // CoreTextField's onValueChange is called multiple times without recomposition in between.
    var lastTextValue by remember(text) { mutableStateOf(text) }

    SideEffect {
        if (textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition
        ) {
            textFieldValueState = textFieldValue
        }
    }
    OutlinedTextFieldWithError(
        textFieldValue = textFieldValue,
        modifier = modifier,
        errorText = errorText,
        focusRequester = focusRequester,
        maxLines = maxLines,
    ) { newTextFieldValueState ->
        textFieldValueState = newTextFieldValueState

        val stringChangedSinceLastInvocation = lastTextValue != newTextFieldValueState.text
        lastTextValue = newTextFieldValueState.text

        if (stringChangedSinceLastInvocation) {
            onValueChanged(newTextFieldValueState.text)
        }
    }
}

@Composable
fun OutlinedTextFieldWithError(
    textFieldValue: TextFieldValue,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    focusRequester: FocusRequester = remember { FocusRequester() },
    maxLines: Int = MaxLines,
    onValueChanged: (TextFieldValue) -> Unit,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = onValueChanged,
            maxLines = maxLines,
            modifier = Modifier.focusRequester(focusRequester),
            isError = errorText != null,
            textStyle = ProtonTheme.typography.default,
            colors = TextFieldDefaults.protonOutlineTextFieldColors()
        )
        Text(
            text = errorText ?: "",
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = ProtonTheme.typography.caption,
            color = ProtonTheme.colors.notificationError
        )
    }
}

private const val MaxLines = 2
