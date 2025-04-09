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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.android.drive.photos.domain.usecase.AddPhotosToAlbum
import me.proton.android.drive.photos.domain.usecase.GetPhotoListingCount
import me.proton.android.drive.photos.domain.usecase.RemoveFromAlbumInfo
import me.proton.android.drive.photos.presentation.extension.processAdd
import me.proton.android.drive.ui.viewevent.PickerPhotosAndAlbumsViewEvent
import me.proton.android.drive.ui.viewstate.PickerPhotosAndAlbumsViewState
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class PickerPhotosAndAlbumsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getPhotoListingCount: GetPhotoListingCount,
    @ApplicationContext private val appContext: Context,
    private val removeFromAlbumInfo: RemoveFromAlbumInfo,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
    private val addPhotosToAlbum: AddPhotosToAlbum,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    val destinationAlbumId = savedStateHandle.get<String?>(DESTINATION_SHARE_ID)?.let { destinationShareId ->
        savedStateHandle.get<String?>(DESTINATION_ALBUM_ID)?.let { destinationAlbumId ->
            AlbumId(ShareId(userId, destinationShareId), destinationAlbumId)
        }
    }
    private val addingInProgress = MutableStateFlow(false)
    private val photoListingsCount: StateFlow<Int?> = getPhotoListingCount(userId, destinationAlbumId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val viewState = combine(
        addingInProgress,
        photoListingsCount.filterNotNull(),
    ) { inProgress, count ->
        PickerPhotosAndAlbumsViewState(
            addToAlbumButtonTitle = if (count == 0) {
                appContext.getString(I18N.string.albums_add_zero_to_album_button)
            } else {
                appContext.quantityString(
                    pluralRes = I18N.plurals.albums_add_non_zero_to_album_button,
                    quantity = count,
                )
            },
            isAddToAlbumButtonEnabled = (count > 0) && !inProgress,
            isResetButtonEnabled = (count > 0) && !inProgress,
            isAddingInProgress = inProgress,
        )
    }

    fun viewEvent(
        navigateBack: () -> Unit,
        onAddToAlbumDone: () -> Unit,
    ): PickerPhotosAndAlbumsViewEvent = object : PickerPhotosAndAlbumsViewEvent {
        override val onBackPressed = { navigateBack() }
        override val onReset = this@PickerPhotosAndAlbumsViewModel::onReset
        override val onAddToAlbum = { onAddToAlbum(onAddToAlbumDone) }
    }

    private fun onAddToAlbum(onDone: () -> Unit) {
        viewModelScope.launch {
            if (destinationAlbumId == null) {
                onDone()
            } else {
                // add photos from add to album info into destination album
                addingInProgress.value = true
                addPhotosToAlbum(destinationAlbumId)
                    .onFailure { error ->
                        addingInProgress.value = false
                        error.log(VIEW_MODEL, "Failed adding photos to album")
                        broadcastMessages(
                            userId = userId,
                            message = error.getDefaultMessage(
                                context = appContext,
                                useExceptionMessage = configurationProvider.useExceptionMessage
                            ),
                            type = BroadcastMessage.Type.ERROR,
                        )
                    }
                    .onSuccess { result ->
                        addingInProgress.value = false
                        result
                            .processAdd(appContext) { message, type ->
                                broadcastMessages(
                                    userId = userId,
                                    message = message,
                                    type = type,
                                )
                            }
                        onDone()
                    }
            }
        }
    }

    private fun onReset() {
        viewModelScope.launch {
            removeFromAlbumInfo(userId, destinationAlbumId)
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Failed removing all photo listings from add to album")
                    broadcastMessages(
                        userId = userId,
                        message = error.getDefaultMessage(
                            context = appContext,
                            useExceptionMessage = configurationProvider.useExceptionMessage
                        )
                    )
                }
        }
    }

    companion object {
        const val DESTINATION_SHARE_ID = "destinationShareId"
        const val DESTINATION_ALBUM_ID = "destinationAlbumId"
    }
}
