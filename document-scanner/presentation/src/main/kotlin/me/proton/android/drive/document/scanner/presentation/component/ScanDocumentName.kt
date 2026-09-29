/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.android.drive.document.scanner.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proton.android.drive.document.scanner.presentation.entity.ScannerOutputFormat
import me.proton.android.drive.document.scanner.presentation.viewevent.ScanDocumentNameViewEvent
import me.proton.android.drive.document.scanner.presentation.viewstate.ScanDocumentNameViewState
import me.proton.android.drive.photos.presentation.component.protonTextFieldColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.DefaultButtonMinHeight
import me.proton.core.compose.theme.ProtonDimens.DefaultIconSize
import me.proton.core.compose.theme.ProtonDimens.ExtraSmallSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.drive.i18n.R as I18N


@Composable
fun ScanDocumentName(
    viewState: ScanDocumentNameViewState,
    viewEvent: ScanDocumentNameViewEvent,
    modifier: Modifier = Modifier,
) {
    val documentName by viewState.name.collectAsStateWithLifecycle(
        initialValue = null
    )
    Column(modifier = modifier) {
        documentName?.let { name ->
            ScanDocumentName(
                name = name,
                selectAllName = viewState.selectAllName,
                isNameEnabled = true,
                onValueChanged = viewEvent.onNameChanged,
                onDone = viewEvent.onDone,
            )
        }
        ScannerOutputFormats(
            outputFormats = viewState.outputFormats,
            selectedOutputFormats = viewState.selectedOutputFormats,
            onScannerOutputFormat = viewEvent.onScannerOutputFormat,
        )
    }
}

@Composable
fun ScanDocumentName(
    name: String,
    selectAllName: Boolean,
    isNameEnabled: Boolean,
    modifier: Modifier = Modifier,
    onValueChanged: (String) -> Unit,
    onDone: () -> Unit,
) {
    val state = remember {
        mutableStateOf(
            TextFieldValue(
                text = name,
                selection = if (selectAllName) {
                    TextRange(0, name.length)
                } else {
                    TextRange(name.length, name.length)
                },
            )
        )
    }
    val focusRequester: FocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScanDocumentName(
        textFieldValue = state.value,
        focusRequester = focusRequester,
        isEnabled = isNameEnabled,
        modifier = modifier,
        onDone = onDone,
    ) { textField: TextFieldValue ->
        if (textField.text != state.value.text) {
            onValueChanged(textField.text)
        }
        state.value = textField
    }
}

@Composable
fun ScanDocumentName(
    textFieldValue: TextFieldValue,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    hint: String? = null,
    maxLines: Int = MaxLines,
    isEnabled: Boolean,
    onDone: () -> Unit,
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
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            capitalization = KeyboardCapitalization.Sentences,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone() }
        ),
        maxLines = maxLines,
        modifier = modifier
            .focusRequester(focusRequester),
        textStyle = ProtonTheme.typography.body1Regular,
        colors = TextFieldDefaults.protonTextFieldColors(),
        enabled = isEnabled,
    )
}

@Composable
private fun ScannerOutputFormats(
    outputFormats: List<ScannerOutputFormat>,
    selectedOutputFormats: List<ScannerOutputFormat>,
    modifier: Modifier = Modifier,
    onScannerOutputFormat: (ScannerOutputFormat) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(I18N.string.scan_document_name_output_formats_section),
            color = ProtonTheme.colors.brandNorm,
            style = ProtonTheme.typography.body1Medium,
            modifier = Modifier.padding(start = ProtonDimens.DefaultSpacing, top = ProtonDimens.DefaultSpacing),
        )
        outputFormats.forEach { scannerOutputFormat ->
            ScannerFormatCheckbox(
                checked = scannerOutputFormat in selectedOutputFormats,
                label = scannerOutputFormat.title,
                description = scannerOutputFormat.description,
                onCheckedChange = { onScannerOutputFormat(scannerOutputFormat) }
            )
        }
    }
}

@Composable
private fun ScannerFormatCheckbox(
    checked: Boolean,
    label: String,
    description: String,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .clickable { onCheckedChange() }
            .fillMaxWidth()
            .padding(
                horizontal = ExtraSmallSpacing,
                vertical = SmallSpacing
            ),
        horizontalArrangement = Arrangement.spacedBy(SmallSpacing)
    ) {
        Box(
            modifier = Modifier.size(DefaultButtonMinHeight),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                modifier = Modifier
                    .size(DefaultIconSize),
                checked = checked,
                onCheckedChange = null,
            )
        }
        Column(
            modifier = Modifier
                .align(CenterVertically)
                .weight(1F)
        ) {
            Text(
                text = label,
                style = ProtonTheme.typography.defaultNorm,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = ProtonTheme.typography.captionWeak,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
fun ScanDocumentNamePreview() {
    ProtonTheme {
        ScanDocumentName(
            name = "Scan",
            selectAllName = true,
            isNameEnabled = true,
            onValueChanged = {},
            onDone = {},
            modifier = Modifier.statusBarsPadding(),
        )
    }
}

private const val MaxLines = 3
