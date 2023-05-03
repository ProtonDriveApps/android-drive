/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.drivelink.shared.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.drive.i18n.R as I18N

@Composable
fun DiscardChangesDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ProtonAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            ProtonAlertDialogButton(
                title = stringResource(id = I18N.string.common_discard_action),
                onClick = onConfirm,
            )
        },
        title =  stringResource(id = I18N.string.shared_link_dialog_title_discard_changes),
        dismissButton = {
            ProtonAlertDialogButton(
                title = stringResource(id = I18N.string.common_cancel_action),
                onClick = onDismiss,
            )
        },
        text = {
            ProtonAlertDialogText(
                text = stringResource(id = I18N.string.shared_link_dialog_description)
            )
        },
        modifier = modifier,
    )
}
