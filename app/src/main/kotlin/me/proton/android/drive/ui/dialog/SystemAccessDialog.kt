/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.android.drive.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import me.proton.android.drive.ui.viewmodel.SystemAccessDialogViewModel
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.drive.i18n.R as I18N

@Composable
fun SystemAccessDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<SystemAccessDialogViewModel>()
    SystemAccessDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        onSettings = viewModel.viewEvent(LocalContext.current, onDismiss).onSettings,
    )
}

@Composable
fun SystemAccessDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
) {
    ProtonAlertDialog(
        modifier = modifier,
        titleResId = I18N.string.app_lock_system_dialog_title,
        text = {
            Column {
                ProtonAlertDialogText(textResId = I18N.string.app_lock_system_dialog_description)
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.common_cancel_action,
                onClick = onDismiss,
            )
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.app_lock_system_dialog_settings_button,
                onClick = onSettings,
            )
        }
    )
}
