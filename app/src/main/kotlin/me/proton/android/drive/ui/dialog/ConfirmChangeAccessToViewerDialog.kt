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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import me.proton.android.drive.ui.viewmodel.ConfirmChangeAccessToViewerDialogViewModel
import me.proton.android.drive.ui.viewstate.ConfirmChangeAccessToViewerViewState
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultHint
import me.proton.core.drive.i18n.R as I18N

@Composable
@ExperimentalCoroutinesApi
fun ConfirmChangeAccessToViewerDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val viewModel = hiltViewModel<ConfirmChangeAccessToViewerDialogViewModel>()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle(initialValue = viewModel.initialViewState)
    val viewEvent = remember(onDismiss, viewModel) { viewModel.viewEvent(onDismiss) }

    ConfirmChangeAccessToViewerDialog(
        viewState = viewState,
        onDismiss = onDismiss,
        onConfirm = viewEvent.onConfirm,
        modifier = modifier
            .testTag(ConfirmChangeAccessToViewerDialogTestTag.confirmChangeAccessToViewer),
    )
}

@Composable
private fun ConfirmChangeAccessToViewerDialog(
    viewState: ConfirmChangeAccessToViewerViewState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProtonAlertDialog(
        modifier = modifier,
        titleResId = I18N.string.manage_access_change_access_to_viewer_title,
        text = {
            Column {
                ProtonAlertDialogText(text = viewState.description)
                viewState.errorMessage?.let { errorMessage ->
                    Text(
                        modifier = Modifier.padding(top = SmallSpacing),
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
                titleResId = I18N.string.common_cancel_action,
                onClick = onDismiss,
                enabled = !viewState.isLoading,
            )
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = I18N.string.manage_access_change_access_to_viewer_action,
                onClick = onConfirm,
                loading = viewState.isLoading,
            )
        }
    )
}

@Preview
@Composable
private fun ConfirmChangeAccessToViewerDialogPreview() {
    ProtonTheme {
        ConfirmChangeAccessToViewerDialog(
            viewState = ConfirmChangeAccessToViewerViewState(
                description = "You won't be able to share \"My folder\" after changing access to " +
                    "viewer.\nYou cannot undo this action.",
            ),
            onDismiss = {},
            onConfirm = {},
        )
    }
}

object ConfirmChangeAccessToViewerDialogTestTag {
    const val confirmChangeAccessToViewer = "confirm change access to viewer"
}
