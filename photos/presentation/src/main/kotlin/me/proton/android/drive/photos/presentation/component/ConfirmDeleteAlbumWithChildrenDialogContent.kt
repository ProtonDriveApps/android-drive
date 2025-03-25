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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.proton.android.drive.photos.presentation.viewevent.ConfirmDeleteAlbumWithChildrenViewEvent
import me.proton.android.drive.photos.presentation.viewstate.ConfirmDeleteAlbumWithChildrenViewState
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.component.ProtonAlertDialogText

@Composable
fun ConfirmDeleteAlbumWithChildrenDialogContent(
    viewState: ConfirmDeleteAlbumWithChildrenViewState,
    viewEvent: ConfirmDeleteAlbumWithChildrenViewEvent,
    modifier: Modifier = Modifier,
) {
    ConfirmDeleteAlbumWithChildrenDialogContent(
        titleResId = viewState.titleResId,
        descriptionResId = viewState.descriptionResId,
        dismissButtonResId = viewState.dismissButtonResId,
        deleteWithoutSavingButtonResId = viewState.confirmWithoutSavingButtonResId,
        saveAndDeleteButtonResId = viewState.confirmSaveAndDeleteButtonResId,
        isWithoutSavingOperationInProgress = viewState.isWithoutSavingOperationInProgress,
        isSavingOperationInProgress = viewState.isSavingOperationInProgress,
        modifier = modifier,
        onDismiss = viewEvent.onDismiss,
        onDeleteWithoutSaving = viewEvent.onDeleteAlbumWithChildren,
        onSaveAndDelete = viewEvent.onSaveAndDeleteAlbum,
    )
}

@Composable
fun ConfirmDeleteAlbumWithChildrenDialogContent(
    titleResId: Int,
    descriptionResId: Int,
    dismissButtonResId: Int,
    deleteWithoutSavingButtonResId: Int,
    saveAndDeleteButtonResId: Int,
    isWithoutSavingOperationInProgress: Boolean,
    isSavingOperationInProgress: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onDeleteWithoutSaving: () -> Unit,
    onSaveAndDelete: () -> Unit,
) {
    ProtonAlertDialog(
        modifier = modifier,
        titleResId = titleResId,
        text = { ProtonAlertDialogText(text = stringResource(descriptionResId)) },
        onDismissRequest = onDismiss,
        confirmButton = {
            DeleteAlbumWithChildrenDialogButtons(
                dismissButtonResId = dismissButtonResId,
                deleteWithoutSavingButtonResId = deleteWithoutSavingButtonResId,
                saveAndDeleteButtonResId = saveAndDeleteButtonResId,
                isWithoutSavingOperationInProgress = isWithoutSavingOperationInProgress,
                isSavingOperationInProgress = isSavingOperationInProgress,
                onDismiss = onDismiss,
                onDeleteWithoutSaving = onDeleteWithoutSaving,
                onSaveAndDelete = onSaveAndDelete,
            )
        },
    )
}

@Composable
private fun DeleteAlbumWithChildrenDialogButtons(
    dismissButtonResId: Int,
    deleteWithoutSavingButtonResId: Int,
    saveAndDeleteButtonResId: Int,
    isWithoutSavingOperationInProgress: Boolean,
    isSavingOperationInProgress: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onDeleteWithoutSaving: () -> Unit,
    onSaveAndDelete: () -> Unit,
) {
    val isOperationInProgress = isWithoutSavingOperationInProgress || isSavingOperationInProgress
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            ProtonAlertDialogButton(
                titleResId = dismissButtonResId,
                onClick = onDismiss,
            )
            ProtonAlertDialogButton(
                titleResId = deleteWithoutSavingButtonResId,
                loading = isWithoutSavingOperationInProgress,
                enabled = !isOperationInProgress,
                onClick = onDeleteWithoutSaving,
            )
            ProtonAlertDialogButton(
                titleResId = saveAndDeleteButtonResId,
                loading = isSavingOperationInProgress,
                enabled = !isOperationInProgress,
                onClick = onSaveAndDelete,
            )
        }
    }
}
