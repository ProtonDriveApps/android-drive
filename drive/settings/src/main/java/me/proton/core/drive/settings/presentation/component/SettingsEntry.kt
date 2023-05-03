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

package me.proton.core.drive.settings.presentation.component

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.drive.base.presentation.component.protonOutlineTextFieldColors
import me.proton.core.drive.settings.presentation.state.LegalLink
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
fun ExternalSettingsEntry(
    link: LegalLink.External,
    modifier: Modifier = Modifier,
    onLinkClicked: (LegalLink) -> Unit,
) {
    val description = stringResource(I18N.string.settings_external_link_format, link.text)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onLinkClicked(link) }
            .padding(horizontal = DefaultSpacing)
            .semantics(mergeDescendants = false) {
                contentDescription = description
            }
            .sizeIn(minHeight = ProtonDimens.ListItemHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(link.text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = ProtonTheme.typography.default,
            modifier = Modifier.weight(1f),
        )
        Icon(
            modifier = Modifier.align(Alignment.CenterVertically),
            painter = painterResource(CorePresentation.drawable.ic_proton_arrow_out_square),
            tint = ProtonTheme.colors.iconNorm,
            contentDescription = null,
        )
    }
}


@Composable
fun EditableSettingsEntry(
    @StringRes label: Int,
    value: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onUpdate: (String) -> Unit,
) = EditableSettingsEntry(
    label = stringResource(id = label),
    value = value,
    modifier = modifier,
    focusRequester = focusRequester,
    onUpdate = onUpdate,
)

@Composable
fun EditableSettingsEntry(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onUpdate: (String) -> Unit,
) {
    var state by remember(value) { mutableStateOf(TextFieldValue(text = value)) }
    var isEditable by remember { mutableStateOf(false) }
    LaunchedEffect(isEditable) {
        if (isEditable) {
            focusRequester.requestFocus()
            state = TextFieldValue(
                text = state.text,
                selection = TextRange(state.text.length)
            )
        }
    }
    Row(
        modifier = modifier
            .padding(horizontal = DefaultSpacing)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .weight(1f),
            label = { Text(text = label) },
            value = state,
            enabled = isEditable,
            onValueChange = { textField ->
                state = textField
            },
            colors = TextFieldDefaults.protonOutlineTextFieldColors()
        )
        if (isEditable) {
            EditableIconButton(
                iconResId = CorePresentation.drawable.ic_proton_checkmark,
                tint = ProtonTheme.colors.brandNorm,
                modifier = Modifier.padding(start = DefaultSpacing),
            ) {
                onUpdate(state.text)
                isEditable = !isEditable
            }
        } else {
            EditableIconButton(
                iconResId = CorePresentation.drawable.ic_proton_pen,
                tint = ProtonTheme.colors.iconNorm,
                modifier = Modifier.padding(start = DefaultSpacing),
            ) {
                isEditable = !isEditable
            }
        }
    }
}

@Composable
private fun EditableIconButton(
    @DrawableRes iconResId: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = tint
        )
    }
}

@Preview(
    name = "SettingsEntry in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "SettingsEntry in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
private fun PreviewExternalSettingsEntry() {
    ProtonTheme {
        ExternalSettingsEntry(
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
            link = LegalLink.External(
                text = I18N.string.common_app,
                url = I18N.string.settings_section_security,
            )) {
        }
    }
}

@Preview(
    name = "EditableSettingsEntry in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "EditableSettingsEntry in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Suppress("unused")
@Composable
private fun PreviewEditableSettingsEntry() {
    ProtonTheme {
        EditableSettingsEntry(
            modifier = Modifier.background(ProtonTheme.colors.backgroundNorm),
            label = "Label",
            value = "Value"
        ) {

        }
    }
}
