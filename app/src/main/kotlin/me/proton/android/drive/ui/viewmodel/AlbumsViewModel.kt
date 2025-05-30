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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.usecase.GetPhotosDriveLink
import me.proton.android.drive.photos.presentation.R
import me.proton.android.drive.photos.presentation.state.AlbumsItem
import me.proton.android.drive.photos.presentation.viewevent.AlbumsViewEvent
import me.proton.android.drive.photos.presentation.viewstate.AlbumsFilter
import me.proton.android.drive.photos.presentation.viewstate.AlbumsViewState
import me.proton.android.drive.ui.effect.HomeEffect
import me.proton.android.drive.ui.effect.HomeTabViewModel
import me.proton.android.drive.usecase.OnFilesDriveLinkError
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccessValueOrNull
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
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.common.getThemeDrawableId
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.base.presentation.viewstate.TagViewState
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoDriveLinks
import me.proton.core.drive.drivelink.photo.domain.usecase.FetchAndStoreAllAlbumListings
import me.proton.core.drive.drivelink.photo.domain.usecase.GetAllAlbumListings
import me.proton.core.drive.drivelink.shared.presentation.viewstate.UserInvitationBannerViewState
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.photo.domain.extension.filterBy
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.user.domain.usecase.GetUserInvitationCountFlow
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation
import me.proton.core.drive.base.presentation.R as BasePresentation

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
    private val getUserInvitationCountFlow: GetUserInvitationCountFlow,
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
    private var viewEvent: AlbumsViewEvent? = null
    private val addAlbumAction = Action.Icon(
        iconResId = CorePresentation.drawable.ic_proton_plus,
        contentDescriptionResId = I18N.string.content_description_albums_new,
        notificationDotVisible = false,
        onAction = { viewEvent?.onCreateNewAlbum?.invoke() },
    )
    private val topBarActions: MutableStateFlow<Set<Action>> =
        MutableStateFlow(setOf(addAlbumAction))
    private val emptyStateImageResId: Int get() = getThemeDrawableId(
        light = BasePresentation.drawable.empty_albums_light,
        dark = BasePresentation.drawable.empty_albums_dark,
        dayNight = BasePresentation.drawable.empty_albums_daynight,
    )

    val initialViewState: AlbumsViewState =
        AlbumsViewState(
            topBarActions = topBarActions,
            listContentState = listContentState.value,
            isRefreshEnabled = listContentState.value != ListContentState.Loading,
            placeholderImageResId = emptyStateImageResId,
            navigationIconResId = CorePresentation.drawable.ic_proton_hamburger,
            filters = listOf(
                AlbumsFilter(
                    AlbumListing.Filter.ALL,
                    TagViewState(
                        label = appContext.getString(I18N.string.albums_filter_all),
                        icon = BasePresentation.drawable.ic_folder_album_outline,
                        selected = true,
                    )
                ),
                AlbumsFilter(
                    AlbumListing.Filter.MY_ALBUMS,
                    TagViewState(
                        label = appContext.getString(I18N.string.albums_filter_my_albums),
                        icon = CorePresentation.drawable.ic_proton_user,
                    )
                ),
                AlbumsFilter(
                    AlbumListing.Filter.SHARED_BY_ME,
                    TagViewState(
                        label = appContext.getString(I18N.string.albums_filter_shared_by_me),
                        icon = CorePresentation.drawable.ic_proton_link,
                    )
                ),
                AlbumsFilter(
                    AlbumListing.Filter.SHARED_WITH_ME,
                    TagViewState(
                        label = appContext.getString(I18N.string.albums_filter_shared_with_me),
                        icon = CorePresentation.drawable.ic_proton_users,
                    )
                ),
            )
        )
    val userInvitationBannerViewState = getUserInvitationCountFlow(
        userId = userId,
        albumsOnly = true,
        refresh = flowOf(true),
    ).filterSuccessOrError()
        .mapSuccessValueOrNull()
        .filterNotNull()
        .filter { count -> count > 0 }
        .map { count ->
            UserInvitationBannerViewState(
                appContext.quantityString(
                    I18N.plurals.shared_with_me_album_invitations_banner_description,
                    count
                )
            )
        }

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
                        listContentState.value = when (albumListingsFilter.value) {
                            AlbumListing.Filter.ALL -> ListContentState.Empty(
                                imageResId = emptyStateImageResId,
                                titleId = I18N.string.albums_empty_albums_list_screen_title,
                                descriptionResId = I18N.string.albums_empty_albums_list_screen_description,
                                actionResId = I18N.string.common_create_album_action
                            )

                            AlbumListing.Filter.MY_ALBUMS -> ListContentState.Empty(
                                imageResId = emptyStateImageResId,
                                titleId = I18N.string.albums_empty_albums_list_screen_title,
                                descriptionResId = I18N.string.albums_empty_albums_my_albums_screen_description,
                                actionResId = I18N.string.common_create_album_action
                            )

                            AlbumListing.Filter.SHARED_BY_ME -> ListContentState.Empty(
                                imageResId = emptyStateImageResId,
                                titleId = I18N.string.albums_empty_albums_shared_by_me_screen_title,
                                descriptionResId = I18N.string.albums_empty_albums_shared_by_me_screen_description,
                                actionResId = I18N.string.common_create_album_action
                            )

                            AlbumListing.Filter.SHARED_WITH_ME -> ListContentState.Empty(
                                imageResId = emptyStateImageResId,
                                titleId = I18N.string.albums_empty_albums_shared_with_me_screen_title,
                                descriptionResId = I18N.string.albums_empty_albums_shared_with_me_screen_description,
                                actionResId = null
                            )
                        }
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
        albumListingsFilter,
    ) { state, refreshing, albumListingsFilter ->
        initialViewState.copy(
            listContentState = when (state) {
                is ListContentState.Content -> state.copy(isRefreshing = refreshing)
                is ListContentState.Empty -> state.copy(isRefreshing = refreshing)
                is ListContentState.Error -> state.copy(isRefreshing = refreshing)
                else -> state
            },
            isRefreshEnabled = state != ListContentState.Loading,
            filters = initialViewState.filters.map { filter ->
                filter.copy(
                    tagViewState = filter.tagViewState.copy(
                        selected = filter.filter == albumListingsFilter
                    )
                )
            }
        )
    }

    fun viewEvent(
        navigateToCreateNewAlbum: () -> Unit,
        navigateToAlbum: (AlbumId) -> Unit,
        navigateToUserInvitation: (Boolean) -> Unit,
    ): AlbumsViewEvent = object : AlbumsViewEvent {
        override val onRefresh = this@AlbumsViewModel::onRefresh
        override val onScroll = this@AlbumsViewModel::onScroll
        override val onErrorAction = this@AlbumsViewModel::onErrorAction
        override val onTopAppBarNavigation = {
            viewModelScope.launch { _homeEffect.emit(HomeEffect.OpenDrawer) }
            Unit
        }
        override val onDriveLinkAlbum = { album: DriveLink.Album ->
            navigateToAlbum(album.id)
        }
        override val onCreateNewAlbum = { navigateToCreateNewAlbum() }
        override val onFilterSelected = this@AlbumsViewModel::onFilterSelected
        override val onUserInvitation = { navigateToUserInvitation(true) }
    }.also { viewEvent ->
        this.viewEvent = viewEvent
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
                photoDriveLinks.load(driveLinkIds)
            }
        }
    }

    private fun onErrorAction() {
        viewModelScope.launch {
            retryTrigger.emit(Unit)
        }
    }

    private fun onFilterSelected(filter: AlbumListing.Filter) {
        viewModelScope.launch {
            albumListingsFilter.emit(filter)
        }
    }
}
