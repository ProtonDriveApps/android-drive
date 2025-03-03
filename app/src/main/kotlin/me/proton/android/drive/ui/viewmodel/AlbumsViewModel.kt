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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.usecase.GetPhotosDriveLink
import me.proton.android.drive.photos.presentation.state.AlbumsItem
import me.proton.android.drive.photos.presentation.viewevent.AlbumsViewEvent
import me.proton.android.drive.photos.presentation.viewstate.AlbumsViewState
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.android.drive.usecase.OnFilesDriveLinkError
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.isRetryable
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoDriveLinks
import me.proton.core.drive.drivelink.photo.domain.usecase.FetchAndStoreAllAlbumListings
import me.proton.core.drive.drivelink.photo.domain.usecase.GetAllAlbumListings
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.extension.filterBy
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val getAllAlbumListings: GetAllAlbumListings,
    private val fetchAndStoreAllAlbumListings: FetchAndStoreAllAlbumListings,
    private val configurationProvider: ConfigurationProvider,
    private val photoDriveLinks: PhotoDriveLinks,
    private val getPhotosDriveLink: GetPhotosDriveLink,
    private val onFilesDriveLinkError: OnFilesDriveLinkError,
    private val broadcastMessages: BroadcastMessages,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle), HomeTabViewModel {
    private var fetchingJob: Job? = null
    private val _homeEffect = MutableSharedFlow<HomeEffect>()
    override val homeEffect: Flow<HomeEffect>
        get() = _homeEffect.asSharedFlow()
    private val listContentState: MutableStateFlow<ListContentState> =
        MutableStateFlow(ListContentState.Loading)
    private val isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val driveLinksMap: StateFlow<Map<LinkId, DriveLink>> =
        photoDriveLinks.getDriveLinksMapFlow(userId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    private val albumListingsFilter: MutableStateFlow<AlbumListing.Filter> =
        MutableStateFlow(AlbumListing.Filter.ALL)

    val initialViewState: AlbumsViewState =
        AlbumsViewState(
            listContentState = listContentState.value,
            isRefreshEnabled = listContentState.value != ListContentState.Loading,
            navigationIconResId = CorePresentation.drawable.ic_proton_hamburger,
        )

    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val driveLink: StateFlow<DriveLink.Folder?> = retryTrigger.transformLatest {
        emitAll(
            getPhotosDriveLink(userId)
                .filterSuccessOrError()
                .mapWithPrevious { previous, result ->
                    result
                        .onSuccess { driveLink ->
                            CoreLogger.d(VIEW_MODEL, "drive link onSuccess")
                            //parentFolderId.value = driveLink.id
                            return@mapWithPrevious driveLink
                        }
                        .onFailure { error ->
                            onFilesDriveLinkError(
                                userId = userId,
                                previous = previous,
                                error = error,
                                contentState = listContentState,
                                shareType = Share.Type.PHOTO,
                            )
                            error.log(VIEW_MODEL, "Cannot get drive link")
                        }
                    return@mapWithPrevious null
                }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val albumListings = driveLink
        .filterNotNull()
        .transform { folder ->
            emitAll(
                getAllAlbumListings(userId, folder.volumeId, filterBy = albumListingsFilter)
            )
        }
        .transform { result ->
            when (result) {
                is DataResult.Processing -> listContentState.value = ListContentState.Loading
                is DataResult.Error -> listContentState.value = ListContentState.Error(
                    message = result.getDefaultMessage(
                        context = appContext,
                        useExceptionMessage = configurationProvider.useExceptionMessage,
                    ),
                    actionResId = if (result.isRetryable) I18N.string.common_retry_action else null,
                )
                is DataResult.Success -> emit(result.value).also {
                    if (result.value.isEmpty()) {
                        listContentState.value = ListContentState.Empty(0, 0)
                    } else {
                        listContentState.value = ListContentState.Content()
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val albumItems = combine(
        albumListings,
        driveLinksMap,
        albumListingsFilter,
        driveLink.filterNotNull(),
    ) { listings, links, filter, root ->
        listings
            .filterBy(filter, root.volumeId)
            .map { albumListing ->
                AlbumsItem.Listing(
                    id = albumListing.albumId,
                    isLocked = albumListing.isLocked,
                    photoCount = albumListing.photoCount,
                    lastActivityTime = albumListing.lastActivityTime,
                    isShared = albumListing.isShared,
                    album = links[albumListing.albumId] as? DriveLink.Album,
                    coverLink = albumListing.coverLinkId?.let { coverLinkId ->
                        links[coverLinkId] as? DriveLink.File
                    },
                )
            }
    }

    val viewState: Flow<AlbumsViewState> = combine(
        listContentState,
        isRefreshing,
    ) { state, refreshing ->
        initialViewState.copy(
            listContentState = when (state) {
                is ListContentState.Content -> state.copy(isRefreshing = refreshing)
                is ListContentState.Empty -> state.copy(isRefreshing = refreshing)
                is ListContentState.Error -> state.copy(isRefreshing = refreshing)
                else -> state
            },
            isRefreshEnabled = state != ListContentState.Loading,
        )
    }

    fun viewEvent(): AlbumsViewEvent = object : AlbumsViewEvent {
        override val onRefresh = this@AlbumsViewModel::onRefresh
        override val onScroll = this@AlbumsViewModel::onScroll
        override val onErrorAction = this@AlbumsViewModel::onErrorAction
        override val onTopAppBarNavigation = {
            viewModelScope.launch { _homeEffect.emit(HomeEffect.OpenDrawer) }
            Unit
        }
        override val onDriveLinkAlbum = { _: DriveLink.Album ->
            broadcastMessages(
                userId = userId,
                message = "Not implemented yet",
                type = BroadcastMessage.Type.INFO,
            )
        }
    }

    private fun onRefresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            driveLink.firstOrNull()?.let { rootFolder ->
                fetchAndStoreAllAlbumListings(
                    userId = userId,
                    volumeId = rootFolder.volumeId,
                    shareId = rootFolder.shareId,
                )
                    .onFailure { error ->
                        isRefreshing.value = false
                        error.log(VIEW_MODEL)
                        broadcastMessages(
                            userId = userId,
                            message = error.getDefaultMessage(
                                appContext,
                                configurationProvider.useExceptionMessage
                            ),
                            type = BroadcastMessage.Type.ERROR,
                        )
                    }
                    .onSuccess {
                        isRefreshing.value = false
                    }
            }
        }
    }

    private fun onScroll(driveLinkIds: Set<LinkId>) {
        if (driveLinkIds.isNotEmpty()) {
            fetchingJob?.cancel()
            fetchingJob = viewModelScope.launch {
                delay(300.milliseconds)
                photoDriveLinks.load(driveLinkIds)
            }
        }
    }

    private fun onErrorAction() {
        viewModelScope.launch {
            retryTrigger.emit(Unit)
        }
    }
}
