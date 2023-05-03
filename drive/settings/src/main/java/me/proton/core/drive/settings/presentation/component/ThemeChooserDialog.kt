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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.drive.i18n.R as I18N

@Composable
fun ThemeChooserDialog(
    selectedStyle: Int,
    availableStyles: List<Int>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onThemeSelected: (Int) -> Unit,
) {
    ProtonAlertDialog(
        titleResId = I18N.string.settings_theme_entry,
        modifier = modifier,
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.settings_theme_dialog_cancel_action,
                onClick = onDismiss,
            )
        },
        onDismissRequest = onDismiss,
        text = {
            Column {
                Spacer(Modifier.size(DefaultSpacing))
                availableStyles.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(style) }
                            .padding(SmallSpacing),
                    ) {
                        RadioButton(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            selected = style == selectedStyle,
                            onClick = { onThemeSelected(style) }
                        )
                        Text(
                            text = stringResource(id = style),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(DefaultSpacing),
                            style = ProtonTheme.typography.default()
                        )
                    }
                }
            }
        },
    )
}
