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

package me.proton.android.drive.photos.presentation.component

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.drive.i18n.R

@Composable
fun ConfirmStopSyncFolderDialogContent(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProtonAlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismiss() },
        titleResId = R.string.settings_photos_backup_folders_confirm_stop_sync_title,
        text = {
            Text(
                text = stringResource(id = R.string.settings_photos_backup_folders_confirm_stop_sync_description)
                    .format(name),
                style = ProtonTheme.typography.defaultWeak,
            )
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.settings_photos_backup_folders_confirm_stop_sync_confirm_action,
                onClick = { onConfirm() },
            )
        },
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.settings_photos_backup_folders_confirm_stop_sync_dismiss_action,
                onClick = { onDismiss() },
            )
        }
    )
}


@Preview
@Composable
private fun ConfirmStopSyncFolderDialogContentPreview() {
    ProtonTheme {
        ConfirmStopSyncFolderDialogContent(
            name = "Camera",
            onDismiss = {},
            onConfirm = {}
        )
    }
}
