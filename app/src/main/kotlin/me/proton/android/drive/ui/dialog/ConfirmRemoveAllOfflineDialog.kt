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

package me.proton.android.drive.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.ui.viewmodel.ConfirmRemoveAllOfflineDialogViewModel
import me.proton.core.drive.base.presentation.component.ProtonAlertDialog
import me.proton.core.drive.base.presentation.component.ProtonAlertDialogButton
import me.proton.core.drive.base.presentation.component.ProtonAlertDialogText
import me.proton.core.drive.i18n.R as I18N

@Composable
@ExperimentalCoroutinesApi
fun ConfirmRemoveAllOfflineDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ConfirmRemoveAllOfflineDialogViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val viewEvent = remember(viewModel, onDismiss) { viewModel.viewEvent(dismiss = onDismiss) }

    ProtonAlertDialog(
        modifier = modifier,
        titleResId = I18N.string.files_confirm_remove_all_offline_title,
        text = {
            ProtonAlertDialogText(textResId = I18N.string.files_confirm_remove_all_offline_message)
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.common_cancel_action,
                onClick = viewEvent.onCancel,
                enabled = !viewState.isLoading,
            )
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.files_confirm_remove_all_offline_confirm_action,
                onClick = viewEvent.onConfirm,
                loading = viewState.isLoading,
            )
        },
    )
}
