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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.proton.android.drive.photos.presentation.viewevent.ConfirmLeaveAlbumDialogViewEvent
import me.proton.android.drive.photos.presentation.viewstate.ConfirmLeaveAlbumDialogViewState
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText
import me.proton.core.compose.theme.ProtonDimens

@Composable
fun ConfirmLeaveAlbumDialogContent(
    viewState: ConfirmLeaveAlbumDialogViewState,
    viewEvent: ConfirmLeaveAlbumDialogViewEvent,
    modifier: Modifier = Modifier,
) {
    ConfirmLeaveAlbumDialogContent(
        title = viewState.title,
        text = viewState.description,
        dismissButtonResId = viewState.dismissButtonResId,
        leaveWithoutSavingButtonResId = viewState.confirmWithoutSavingButtonResId,
        saveAndLeaveButtonResId = viewState.confirmSaveAndLeaveButtonResId,
        isWithoutSavingOperationInProgress = viewState.isWithoutSavingOperationInProgress,
        isSavingOperationInProgress = viewState.isSavingOperationInProgress,
        isSaveAndLeaveButtonVisible = viewState.isSaveAndLeaveButtonVisible,
        onDismiss = viewEvent.onDismiss,
        onLeavingWithoutSaving = viewEvent.onLeaveAlbumWithoutSaving,
        onSaveAndLeave = viewEvent.onSaveAndLeaveAlbum,
        modifier = modifier,
    )
}

@Composable
fun ConfirmLeaveAlbumDialogContent(
    title: String,
    text: String,
    dismissButtonResId: Int,
    leaveWithoutSavingButtonResId: Int,
    saveAndLeaveButtonResId: Int,
    isWithoutSavingOperationInProgress: Boolean,
    isSavingOperationInProgress: Boolean,
    isSaveAndLeaveButtonVisible: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onLeavingWithoutSaving: () -> Unit,
    onSaveAndLeave: () -> Unit,
) {
    ProtonAlertDialog(
        title = title,
        onDismissRequest = onDismiss,
        confirmButton = {
            LeaveAlbumDialogButtons(
                dismissButtonResId = dismissButtonResId,
                leaveWithoutSavingButtonResId = leaveWithoutSavingButtonResId,
                saveAndLeaveButtonResId = saveAndLeaveButtonResId,
                isWithoutSavingOperationInProgress = isWithoutSavingOperationInProgress,
                isSavingOperationInProgress = isSavingOperationInProgress,
                isSaveAndLeaveButtonVisible = isSaveAndLeaveButtonVisible,
                onDismiss = onDismiss,
                onLeavingWithoutSaving = onLeavingWithoutSaving,
                onSaveAndLeave = onSaveAndLeave,
            )
        },
        modifier = modifier,
        text = { ProtonAlertDialogText(text = text) },
    )
}

@Composable
fun LeaveAlbumDialogButtons(
    dismissButtonResId: Int,
    leaveWithoutSavingButtonResId: Int,
    saveAndLeaveButtonResId: Int,
    isWithoutSavingOperationInProgress: Boolean,
    isSavingOperationInProgress: Boolean,
    isSaveAndLeaveButtonVisible: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onLeavingWithoutSaving: () -> Unit,
    onSaveAndLeave: () -> Unit,
) {
    val isOperationInProgress = isWithoutSavingOperationInProgress || isSavingOperationInProgress
    Box(modifier = modifier) {
        if (isSaveAndLeaveButtonVisible) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                ProtonAlertDialogButton(
                    titleResId = dismissButtonResId,
                    onClick = onDismiss,
                )
                ProtonAlertDialogButton(
                    titleResId = leaveWithoutSavingButtonResId,
                    loading = isWithoutSavingOperationInProgress,
                    enabled = !isOperationInProgress,
                    onClick = onLeavingWithoutSaving,
                )
                ProtonAlertDialogButton(
                    titleResId = saveAndLeaveButtonResId,
                    loading = isSavingOperationInProgress,
                    enabled = !isOperationInProgress,
                    onClick = onSaveAndLeave,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                ProtonAlertDialogButton(
                    titleResId = dismissButtonResId,
                    onClick = onDismiss,
                )
                Spacer(modifier = Modifier.width(ProtonDimens.DefaultSpacing))
                ProtonAlertDialogButton(
                    titleResId = leaveWithoutSavingButtonResId,
                    loading = isWithoutSavingOperationInProgress,
                    enabled = !isOperationInProgress,
                    onClick = onLeavingWithoutSaving,
                )
            }
        }
    }
}
