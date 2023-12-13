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

package me.proton.android.drive.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.navigation.Screen
import me.proton.android.drive.ui.viewevent.ConfirmDeletionViewEvent
import me.proton.android.drive.ui.viewstate.ConfirmDeletionViewState
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.trash.domain.usecase.DeleteFromTrash
import javax.inject.Inject

@HiltViewModel
@ExperimentalCoroutinesApi
class ConfirmDeletionDialogViewModel @Inject constructor(
    private val deleteFromTrash: DeleteFromTrash,
    private val getDriveLink: GetDecryptedDriveLink,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    val shareId = ShareId(userId, savedStateHandle.require(Screen.Files.Dialogs.ConfirmDeletion.SHARE_ID))
    val linkId: LinkId = FileId(shareId, savedStateHandle.require(Screen.Files.Dialogs.ConfirmDeletion.FILE_ID))

    val initialViewState = ConfirmDeletionViewState(null)
    val viewState = getDriveLink(linkId, failOnDecryptionError = false)
        .filterSuccessOrError()
        .mapSuccessValueOrNull()
        .transform { driveLink ->
            if (driveLink == null) {
                return@transform
            }
            emit(ConfirmDeletionViewState(driveLink.name))
        }
        .shareIn(viewModelScope, SharingStarted.Eagerly)

    fun viewEvent(onDismiss: () -> Unit) = object : ConfirmDeletionViewEvent {
        override val onConfirm = {
            viewModelScope.launch {
                deleteFromTrash(userId, linkId)
                onDismiss()
            }
            Unit
        }
    }
}
