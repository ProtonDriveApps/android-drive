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

package me.proton.core.drive.files.presentation.component.confirmation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.drive.i18n.R as I18N

@Composable
fun ConfirmDeletionDialogContent(
    name: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ProtonAlertDialog(
        modifier = modifier,
        titleResId = I18N.string.title_files_confirm_deletion,
        text = { ProtonAlertDialogText(text = stringResource(id = I18N.string.files_confirm_deletion_message_format, name)) },
        onDismissRequest = onDismiss,
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.common_cancel_action,
                onClick = onDismiss,
            )
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.files_confirm_deletion_confirm_action,
                onClick = onConfirm,
            )
        }
    )
}

@Preview
@Composable
private fun PreviewConfirmDeletionDialogContent() {
    ConfirmDeletionDialogContent(
        name = "test",
        onDismiss = {},
        onConfirm = {},
    )
}
