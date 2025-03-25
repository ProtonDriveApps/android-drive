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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.CombinedLoadStates
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import me.proton.android.drive.photos.domain.usecase.CreateNewAlbum
import me.proton.android.drive.photos.domain.usecase.GetNewAlbumName
import me.proton.android.drive.photos.domain.usecase.GetPagedAddToAlbumPhotoListings
import me.proton.android.drive.photos.domain.usecase.RemoveFromAlbumInfo
import me.proton.android.drive.photos.domain.usecase.UpdateAlbumName
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.CreateNewAlbumViewEvent
import me.proton.android.drive.photos.presentation.viewstate.CreateNewAlbumViewState
import me.proton.core.drive.base.domain.extension.flowOf
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.toVolumePhotoListing
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoDriveLinks
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class CreateNewAlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
    private val updateAlbumName: UpdateAlbumName,
    private val clearNewAlbum: ClearNewAlbum,
    private val getNewAlbumName: GetNewAlbumName,
    private val getPagedAddToAlbumPhotoListings: GetPagedAddToAlbumPhotoListings,
    private val photoDriveLinks: PhotoDriveLinks,
    private val createNewAlbum: CreateNewAlbum,
    private val removeFromAlbumInfo: RemoveFromAlbumInfo,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val isCreationInProgress = MutableStateFlow(false)
    private val currentAlbumName = MutableStateFlow<String?>(null)
    private val initialAlbumName = flowOf {
        getNewAlbumName(userId)
            .getOrNull(VIEW_MODEL, "Get new album info failed")
            ?: ""
    }
    private var fetchingJob: Job? = null
    private val isDoneEnabled = currentAlbumName.map {
        it.isNullOrEmpty().not()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val driveLinksMap: Flow<Map<LinkId, DriveLink>> = photoDriveLinks.getDriveLinksMapFlow(userId)

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

    val photos = getPagedAddToAlbumPhotoListings(userId)
        .map { pagingData ->
            pagingData.map { photoListing ->
                PhotosItem.PhotoListing(
                    id = photoListing.linkId,
                    captureTime = photoListing.captureTime,
                    link = null,
                )
            }
        }
        .cachedIn(viewModelScope)

    fun viewEvent(
        navigateBack: () -> Unit,
        navigateToAlbum: (AlbumId) -> Unit,
    ): CreateNewAlbumViewEvent = object : CreateNewAlbumViewEvent {
        override val onBackPressed = { navigateBack() }
        override val onDone = { onCreateAlbum(navigateToAlbum) }
        override val onNameChanged = ::onChanged
        override val onLoadState = { _: CombinedLoadStates, _: Int -> }
        override val onScroll = this@CreateNewAlbumViewModel::onScroll
        override val onRemove = this@CreateNewAlbumViewModel::onRemove
    }

    private fun onCreateAlbum(navigateToAlbum: (AlbumId) -> Unit) {
        viewModelScope.launch {
            currentAlbumName.value?.let { albumName ->
                isCreationInProgress.value = true
                createNewAlbum(userId = userId, isLocked = false)
                    .onFailure { error ->
                        isCreationInProgress.value = false
                        error.log(VIEW_MODEL, "Creating new album failed")
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
                        navigateToAlbum(albumId)
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

    private fun onScroll(driveLinkIds: Set<LinkId>) {
        if (driveLinkIds.isNotEmpty()) {
            fetchingJob?.cancel()
            fetchingJob = viewModelScope.launch {
                delay(100.milliseconds)
                photoDriveLinks.load(driveLinkIds)
            }
        }
    }

    private fun onRemove(driveLink: DriveLink.File) {
        viewModelScope.launch {
            removeFromAlbumInfo(setOf(driveLink.toVolumePhotoListing()))
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Remove from album info failed")
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(Dispatchers.Main).launch {
            clearNewAlbum(userId)
                .onFailure { error ->
                    error.log(VIEW_MODEL, "Clear new album failed")
                }
        }
    }
}
