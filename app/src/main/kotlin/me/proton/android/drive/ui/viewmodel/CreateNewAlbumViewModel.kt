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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.android.drive.photos.domain.usecase.ClearNewAlbum
import me.proton.android.drive.photos.domain.usecase.GetNewAlbumInfo
import me.proton.android.drive.photos.domain.usecase.UpdateAlbumName
import me.proton.android.drive.photos.presentation.viewevent.CreateNewAlbumViewEvent
import me.proton.android.drive.photos.presentation.viewstate.CreateNewAlbumViewState
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.photo.domain.usecase.CreateAlbum
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class CreateNewAlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val createAlbum: CreateAlbum,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
    private val updateAlbumName: UpdateAlbumName,
    private val clearNewAlbum: ClearNewAlbum,
    private val getNewAlbumInfo: GetNewAlbumInfo,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val isCreationInProgress = MutableStateFlow(false)
    private val currentAlbumName = MutableStateFlow<String?>(null)
    private val initialAlbumName = flowOf {
        getNewAlbumInfo(userId)
            .getOrNull(VIEW_MODEL, "Get new album info failed")
            ?.name ?: ""
    }
    private val isDoneEnabled = currentAlbumName.map {
        it.isNullOrEmpty().not()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val initialViewState = CreateNewAlbumViewState(
        isDoneEnabled = true,
        isAlbumNameEnabled = true,
        isAddEnabled = true,
        isCreationInProgress = isCreationInProgress.value,
        name = initialAlbumName,
        hint = appContext.getString(I18N.string.albums_new_album_name_hint),
    )

    val viewState: Flow<CreateNewAlbumViewState> = combine(
        isDoneEnabled,
        isCreationInProgress,
    ) { isEnabled, isInProgress ->
        initialViewState.copy(
            isDoneEnabled = isEnabled,
            isCreationInProgress = isInProgress,
            isAlbumNameEnabled = !isInProgress,
            isAddEnabled = !isInProgress,
        )
    }

    fun viewEvent(
        navigateBack: () -> Unit,
    ): CreateNewAlbumViewEvent = object : CreateNewAlbumViewEvent {
        override val onBackPressed = { onBackPressed(navigateBack) }
        override val onDone = { onCreateAlbum(navigateBack) }
        override val onNameChanged = ::onChanged
    }

    private fun onCreateAlbum(navigateBack: () -> Unit) {
        viewModelScope.launch {
            currentAlbumName.value?.let { albumName ->
                isCreationInProgress.value = true
                createAlbum(userId = userId, albumName = albumName, isLocked = false)
                    .onFailure { error ->
                        isCreationInProgress.value = false
                        error.log(VIEW_MODEL, "Creating album failed")
                        broadcastMessages(
                            userId = userId,
                            message = error.getDefaultMessage(
                                context = appContext,
                                useExceptionMessage = configurationProvider.useExceptionMessage,
                            ),
                            type = BroadcastMessage.Type.ERROR,
                        )
                    }
                    .onSuccess { albumId ->
                        // add photos to album
                        isCreationInProgress.value = false
                        broadcastMessages(
                            userId = userId,
                            message = appContext.getString(I18N.string.albums_new_album_created_successfully),
                            type = BroadcastMessage.Type.INFO,
                        )
                        clearNewAlbum(userId)
                            .onFailure { error ->
                                error.log(VIEW_MODEL, "Clear new album failed")
                            }
                        navigateBack() //TODO: navigate to album screen once available
                    }
            }
        }
    }

    private fun onChanged(name: String) {
        viewModelScope.launch {
            currentAlbumName.value = name
            updateAlbumName(userId, name)
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Updating album name failed")
                }
        }
    }

    private fun onBackPressed(navigateBack: () -> Unit) {
        viewModelScope.launch {
            clearNewAlbum(userId)
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Clear new album failed")
                }
            navigateBack()
        }
    }
}
