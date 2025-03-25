/*
 * Copyright (c) 2025 Proton AG.
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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.proton.android.drive.photos.presentation.viewevent.ConfirmDeleteAlbumViewEvent
import me.proton.android.drive.photos.presentation.viewstate.ConfirmDeleteAlbumViewState
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText

@Composable
fun ConfirmDeleteAlbumDialogContent(
    viewState: ConfirmDeleteAlbumViewState,
    viewEvent: ConfirmDeleteAlbumViewEvent,
    modifier: Modifier = Modifier,
) {
    ConfirmDeleteAlbumDialogContent(
        titleResId = viewState.titleResId,
        descriptionResId = viewState.descriptionResId,
        dismissButtonResId = viewState.dismissButtonResId,
        confirmButtonResId = viewState.confirmButtonResId,
        isOperationInProgress = viewState.isOperationInProgress,
        modifier = modifier,
        onDismiss = viewEvent.onDismiss,
        onConfirm = viewEvent.onDeleteAlbum,
    )
}

@Composable
fun ConfirmDeleteAlbumDialogContent(
    titleResId: Int,
    descriptionResId: Int,
    dismissButtonResId: Int,
    confirmButtonResId: Int,
    isOperationInProgress: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ProtonAlertDialog(
        modifier = modifier,
        titleResId = titleResId,
        text = { ProtonAlertDialogText(text = stringResource(descriptionResId)) },
        onDismissRequest = onDismiss,
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = dismissButtonResId,
                onClick = onDismiss,
            )
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = confirmButtonResId,
                enabled = !isOperationInProgress,
                loading = isOperationInProgress,
                onClick = onConfirm,
            )
        }
    )
}
