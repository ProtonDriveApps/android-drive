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
package me.proton.core.drive.linkupload.presentation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.linkupload.presentation.R as Presentation

@Composable
fun StorageFullDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    ProtonAlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        titleResId = Presentation.string.files_upload_failure_storage_full_title,
        text = {
            ProtonAlertDialogText(textResId = Presentation.string.files_upload_failure_storage_full_description)
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = BasePresentation.string.common_got_it_action,
                onClick = onDismiss,
            )
        },
    )
}
