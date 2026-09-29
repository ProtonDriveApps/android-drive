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
package me.proton.core.drive.settings.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.drive.base.presentation.component.protonOutlineTextFieldColors
import me.proton.core.drive.i18n.R as I18N

@Composable
fun ChangeEnvironmentDialog(
    host: String,
    baseUrl: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onApply: (host: String, baseUrl: String) -> Unit,
) {
    var hostValue by remember { mutableStateOf(host) }
    var baseUrlValue by remember { mutableStateOf(baseUrl) }
    ProtonAlertDialog(
        modifier = modifier,
        titleResId = I18N.string.debug_settings_change_environment,
        text = {
            Column {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = hostValue,
                    onValueChange = { hostValue = it },
                    label = { Text(text = stringResource(id = I18N.string.debug_settings_host)) },
                    colors = TextFieldDefaults.protonOutlineTextFieldColors(),
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DefaultSpacing),
                    value = baseUrlValue,
                    onValueChange = { baseUrlValue = it },
                    label = { Text(text = stringResource(id = I18N.string.debug_settings_base_url)) },
                    colors = TextFieldDefaults.protonOutlineTextFieldColors(),
                )
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            ProtonAlertDialogButton(titleResId = I18N.string.common_cancel_action, onClick = onDismiss)
        },
        confirmButton = {
            ProtonAlertDialogButton(titleResId = I18N.string.debug_settings_change_environment_apply) {
                onApply(hostValue, baseUrlValue)
            }
        }
    )
}
