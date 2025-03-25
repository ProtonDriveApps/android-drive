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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.android.drive.photos.domain.usecase.SaveChildrenAndDeleteAlbum
import me.proton.android.drive.photos.presentation.viewevent.ConfirmDeleteAlbumViewEvent
import me.proton.android.drive.photos.presentation.viewevent.ConfirmDeleteAlbumWithChildrenViewEvent
import me.proton.android.drive.photos.presentation.viewstate.ConfirmDeleteAlbumDialogViewState
import me.proton.android.drive.photos.presentation.viewstate.ConfirmDeleteAlbumViewState
import me.proton.android.drive.photos.presentation.viewstate.ConfirmDeleteAlbumWithChildrenViewState
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.data.workmanager.onProtonHttpException
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.usecase.DeleteAlbum
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class ConfirmDeleteAlbumDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDriveLink,
    @ApplicationContext private val appContext: Context,
    private val deleteAlbum: DeleteAlbum,
    private val saveChildrenAndDeleteAlbum: SaveChildrenAndDeleteAlbum,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val albumId = AlbumId(
        ShareId(userId, requireNotNull(savedStateHandle[SHARE_ID])),
        requireNotNull(savedStateHandle[ALBUM_ID])
    )
    private val driveLink: StateFlow<DriveLink.Album?> = getDriveLink(albumId)
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private var dismiss: (() -> Unit)? = null
    private val withoutChildrenViewState = ConfirmDeleteAlbumViewState(
        titleResId = I18N.string.albums_delete_album_dialog_title,
        descriptionResId = I18N.string.albums_delete_album_dialog_description,
        dismissButtonResId = I18N.string.albums_delete_album_dialog_cancel_action,
        confirmButtonResId = I18N.string.albums_delete_album_dialog_delete_album_action,
    )
    private val withChildrenViewState = ConfirmDeleteAlbumWithChildrenViewState(
        titleResId = I18N.string.albums_delete_album_dialog_title,
        descriptionResId = I18N.string.albums_delete_album_dialog_description_with_children,
        dismissButtonResId = I18N.string.albums_delete_album_dialog_cancel_action,
        confirmSaveAndDeleteButtonResId = I18N.string.albums_delete_album_dialog_save_and_delete_action,
        confirmWithoutSavingButtonResId = I18N.string.albums_delete_album_dialog_delete_without_saving_action,
    )
    val initialViewState = ConfirmDeleteAlbumDialogViewState(
        withoutChildrenViewState = withoutChildrenViewState,
        withChildrenViewState = withChildrenViewState,
        showDialog = ConfirmDeleteAlbumDialogViewState.Dialog.WITHOUT_CHILDREN,
    )

    private val showDialog: MutableStateFlow<ConfirmDeleteAlbumDialogViewState.Dialog> =
        MutableStateFlow(ConfirmDeleteAlbumDialogViewState.Dialog.WITHOUT_CHILDREN)
    private val isOperationInProgress = MutableStateFlow(false)
    private val isSavingOperationInProgress = MutableStateFlow(false)
    private val isWithoutSavingOperationInProgress = MutableStateFlow(false)

    val viewState: Flow<ConfirmDeleteAlbumDialogViewState> = combine(
        showDialog,
        isOperationInProgress,
        isSavingOperationInProgress,
        isWithoutSavingOperationInProgress
    ) { dialog, isOperationInProgress, isSavingOperationInProgress, isWithoutSavingOperationInProgress ->
        initialViewState.copy(
            withoutChildrenViewState = withoutChildrenViewState.copy(
                isOperationInProgress = isOperationInProgress,
            ),
            withChildrenViewState = withChildrenViewState.copy(
                isSavingOperationInProgress = isSavingOperationInProgress,
                isWithoutSavingOperationInProgress = isWithoutSavingOperationInProgress,
            ),
            showDialog = dialog,
        )
    }

    fun viewEvent(
        onDismiss: () -> Unit
    ): ConfirmDeleteAlbumViewEvent = object : ConfirmDeleteAlbumViewEvent {
        override val onDismiss = onDismiss
        override val onDeleteAlbum = this@ConfirmDeleteAlbumDialogViewModel::onConfirmDeleteAlbum
    }.also {
        this.dismiss = onDismiss
    }

    fun viewEventWithChildren(
        onDismiss: () -> Unit
    ): ConfirmDeleteAlbumWithChildrenViewEvent = object : ConfirmDeleteAlbumWithChildrenViewEvent {
        override val onDismiss = onDismiss
        override val onDeleteAlbumWithChildren = this@ConfirmDeleteAlbumDialogViewModel::onConfirmDeleteAlbumWithChildren
        override val onSaveAndDeleteAlbum = this@ConfirmDeleteAlbumDialogViewModel::onConfirmSaveAndDeleteAlbum
    }.also {
        this.dismiss = onDismiss
    }

    private fun onConfirmDeleteAlbum() {
        viewModelScope.launch {
            isOperationInProgress.value = true
            val album = driveLink.filterNotNull().first()
            deleteAlbum(
                volumeId = album.volumeId,
                albumId = album.id,
            )
                .onFailure { error ->
                    isOperationInProgress.value = false
                    error.onProtonHttpException { protonCode: Int ->
                        if (protonCode == ProtonApiCode.RESULTS_IN_DATA_LOSS) {
                            // store linkIds once available
                            showDialog.value = ConfirmDeleteAlbumDialogViewState.Dialog.WITH_CHILDREN
                        }
                        else {
                            error.log(VIEW_MODEL, "Failed to delete album")
                            broadcastMessages(
                                userId = userId,
                                message = error.getDefaultMessage(
                                    context = appContext,
                                    useExceptionMessage = configurationProvider.useExceptionMessage,
                                ),
                                type = BroadcastMessage.Type.ERROR,
                            )
                        }
                    }

                }
                .onSuccess {
                    isOperationInProgress.value = false
                    dismiss?.invoke()
                }
        }
    }

    private fun onConfirmDeleteAlbumWithChildren() {
        viewModelScope.launch {
            isWithoutSavingOperationInProgress.value = true
            val album = driveLink.filterNotNull().first()
            deleteAlbum(
                volumeId = album.volumeId,
                albumId = album.id,
                deleteAlbumPhotos = true,
            )
                .onFailure { error ->
                    isWithoutSavingOperationInProgress.value = false
                    error.log(VIEW_MODEL, "Failed to delete album with children")
                    broadcastMessages(
                        userId = userId,
                        message = error.getDefaultMessage(
                            context = appContext,
                            useExceptionMessage = configurationProvider.useExceptionMessage,
                        ),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
                .onSuccess {
                    isWithoutSavingOperationInProgress.value = false
                    dismiss?.invoke()
                }
        }
    }

    private fun onConfirmSaveAndDeleteAlbum() {
        viewModelScope.launch {
            isSavingOperationInProgress.value = true
            saveChildrenAndDeleteAlbum(
                albumId = albumId,
                children = emptyList(),//TODO get list of Id's
            )
                .onFailure { error ->
                    isSavingOperationInProgress.value = false
                    error.log(VIEW_MODEL, "Failed to save children and delete album")
                    broadcastMessages(
                        userId = userId,
                        message = error.getDefaultMessage(
                            context = appContext,
                            useExceptionMessage = configurationProvider.useExceptionMessage,
                        ),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
                .onSuccess {
                    isSavingOperationInProgress.value = false
                    dismiss?.invoke()
                }
        }
    }

    companion object {
        const val SHARE_ID = "shareId"
        const val ALBUM_ID = "albumId"
    }
}
