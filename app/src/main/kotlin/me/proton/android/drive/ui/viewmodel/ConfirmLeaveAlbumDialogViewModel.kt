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

package me.proton.android.drive.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.photos.domain.usecase.AddPhotosToStream
import me.proton.android.drive.photos.presentation.extension.processAddToStream
import me.proton.android.drive.photos.presentation.viewevent.ConfirmLeaveAlbumDialogViewEvent
import me.proton.android.drive.photos.presentation.viewstate.ConfirmLeaveAlbumDialogViewState
import me.proton.android.drive.usecase.LeaveShare
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class ConfirmLeaveAlbumDialogViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDecryptedDriveLink: GetDecryptedDriveLink,
    private val leaveShare: LeaveShare,
    private val addPhotosToStream: AddPhotosToStream,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val albumId = AlbumId(
        ShareId(userId, requireNotNull(savedStateHandle[SHARE_ID])),
        requireNotNull(savedStateHandle[ALBUM_ID])
    )
    private val album: StateFlow<DriveLink.Album?> = getDecryptedDriveLink(albumId)
        .filterSuccessOrError()
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val isSavingOperationInProgress = MutableStateFlow(false)
    private val isWithoutSavingOperationInProgress = MutableStateFlow(false)
    val initialViewState = ConfirmLeaveAlbumDialogViewState(
        title = appContext.getString(I18N.string.albums_leave_album_dialog_title),
        description = "",
        dismissButtonResId = I18N.string.albums_leave_album_dialog_cancel_action,
        confirmWithoutSavingButtonResId = I18N.string.albums_leave_album_dialog_leave_without_saving_action,
        confirmSaveAndLeaveButtonResId = I18N.string.albums_leave_album_dialog_save_and_leave_action,
        isSavingOperationInProgress = false,
        isWithoutSavingOperationInProgress = false,
        isSaveAndLeaveButtonVisible = true,
    )

    val viewState: Flow<ConfirmLeaveAlbumDialogViewState> = combine(
        album.filterNotNull(),
        isSavingOperationInProgress,
        isWithoutSavingOperationInProgress,
    ) { album, savingOperationInProgress, withoutSavingOperationInProgress ->
        initialViewState.copy(
            description = appContext.getString(I18N.string.albums_leave_album_dialog_description, album.name),
            isSavingOperationInProgress = savingOperationInProgress,
            isWithoutSavingOperationInProgress = withoutSavingOperationInProgress,
            isSaveAndLeaveButtonVisible = album.photoCount < configurationProvider.savePhotoToStreamLimit,
        )
    }

    fun viewEvent(
        onDismiss: () -> Unit
    ): ConfirmLeaveAlbumDialogViewEvent = object : ConfirmLeaveAlbumDialogViewEvent {
        override val onDismiss = onDismiss
        override val onLeaveAlbumWithoutSaving = { leaveAlbumWithoutSaving(onDismiss) }
        override val onSaveAndLeaveAlbum = { saveAndLeaveAlbum(onDismiss) }
    }

    private fun leaveAlbumWithoutSaving(dismiss: () -> Unit) {
        viewModelScope.launch {
            album.value?.let {
                isWithoutSavingOperationInProgress.value = true
                leaveShare(it)
                    .onSuccess {
                        dismiss()
                    }
                isWithoutSavingOperationInProgress.value = false
            }
        }
    }

    private fun saveAndLeaveAlbum(dismiss: () -> Unit) {
        viewModelScope.launch {
            album.value?.let {
                isSavingOperationInProgress.value = true
                addPhotosToStream(albumId).onFailure { error ->
                    error.log(LogTag.ALBUM, "Cannot copy photo to stream: ${albumId.id.logId()}")
                    broadcastMessages(
                        userId = userId,
                        message = error.getDefaultMessage(
                            appContext,
                            configurationProvider.useExceptionMessage
                        ),
                        type = BroadcastMessage.Type.ERROR
                    )
                }.onSuccess { result ->
                    result.processAddToStream(appContext) { message, type ->
                        broadcastMessages(
                            userId = userId,
                            message = message,
                            type = type,
                        )
                    }
                    leaveShare(it)
                        .onSuccess {
                            dismiss()
                        }
                }
                isSavingOperationInProgress.value = false
            }
        }
    }

    companion object {
        const val SHARE_ID = "shareId"
        const val ALBUM_ID = "albumId"
    }
}
