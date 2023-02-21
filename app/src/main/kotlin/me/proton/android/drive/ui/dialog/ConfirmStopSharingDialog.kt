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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.ui.viewmodel.ConfirmStopSharingDialogViewModel
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.flow.rememberFlowWithLifecycle
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultHint
import me.proton.core.drive.base.presentation.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@ExperimentalCoroutinesApi
fun ConfirmStopSharingDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val viewModel = hiltViewModel<ConfirmStopSharingDialogViewModel>()
    val viewState by rememberFlowWithLifecycle(viewModel.viewState).collectAsState(initial = viewModel.initialViewState)
    val viewEvent = remember(onConfirm, viewModel) { viewModel.viewEvent(onConfirm) }

    ProtonAlertDialog(
        modifier = modifier,
        titleResId = R.string.files_stop_sharing_title,
        text = {
            Column {
                ProtonAlertDialogText(textResId = R.string.files_stop_sharing_message)
                viewState.errorMessage?.let { errorMessage ->
                    Text(
                        modifier = Modifier.padding(top =  SmallSpacing),
                        text = errorMessage,
                        color = ProtonTheme.colors.notificationError,
                        style = ProtonTheme.typography.defaultHint,
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.common_cancel_action,
                onClick = onDismiss,
                enabled = !viewState.isLoading,
            )
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.files_stop_sharing_action,
                onClick = viewEvent.onConfirm,
                loading = viewState.isLoading,
            )
        }
    )
}
