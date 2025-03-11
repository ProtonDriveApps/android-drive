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
import androidx.paging.CombinedLoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.presentation.extension.details
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.AlbumViewEvent
import me.proton.android.drive.photos.presentation.viewstate.AlbumViewState
import me.proton.android.drive.usecase.OnFilesDriveLinkError
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.state.ListContentAppendingState
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.base.presentation.viewmodel.onLoadState
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoDriveLinks
import me.proton.core.drive.drivelink.photo.domain.usecase.GetPagedAlbumPhotoListingsList
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
@ExperimentalCoroutinesApi
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDecryptedDriveLink: GetDecryptedDriveLink,
    @ApplicationContext private val appContext: Context,
    private val onFilesDriveLinkError: OnFilesDriveLinkError,
    private val photoDriveLinks: PhotoDriveLinks,
    private val getPagedAlbumPhotoListingsList: GetPagedAlbumPhotoListingsList,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val shareId = ShareId(userId, requireNotNull(savedStateHandle.get<String>(SHARE_ID)))
    private val albumId = AlbumId(shareId, requireNotNull(savedStateHandle[ALBUM_ID]))
    private var fetchingJob: Job? = null
    private val listContentState = MutableStateFlow<ListContentState>(ListContentState.Loading)
    private val listContentAppendingState = MutableStateFlow<ListContentAppendingState>(
        ListContentAppendingState.Idle
    )
    private val _listEffect = MutableSharedFlow<ListEffect>()
    val listEffect: Flow<ListEffect>
        get() = _listEffect.asSharedFlow()
    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val driveLink: StateFlow<DriveLink.Album?> = retryTrigger.transformLatest {
        emitAll(
            getDecryptedDriveLink(albumId, failOnDecryptionError = false)
                .filterSuccessOrError()
                .mapWithPrevious { previous, result ->
                    result
                        .onSuccess { driveLink ->
                            CoreLogger.d(VIEW_MODEL, "drive link onSuccess")
                            driveLink.coverLinkId?.let { coverLinkId ->
                                photoDriveLinks.load(setOf(coverLinkId))
                            }
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
                            error.log(VIEW_MODEL)
                        }
                    return@mapWithPrevious null
                }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val viewState: Flow<AlbumViewState> = combine(
        driveLink.filterNotNull(),
        listContentState,
    ) { album, contentState ->
        AlbumViewState(
            name = album.name,
            details = album.details(appContext),
            coverLinkId = album.coverLinkId,
            listContentState = contentState,
            isRefreshEnabled = contentState !is ListContentState.Loading,
        )
    }

    val driveLinksMap: Flow<Map<LinkId, DriveLink>> = photoDriveLinks.getDriveLinksMapFlow(userId)

    val driveLinks: Flow<PagingData<PhotosItem.PhotoListing>> =
        driveLink
            .filterNotNull()
            .distinctUntilChanged()
            .transformLatest { album ->
                emitAll(
                    getPagedAlbumPhotoListingsList(
                        albumId = album.id,
                        sortingBy = PhotoListing.Album.SortBy.CAPTURED,
                        sortingDirection = Direction.DESCENDING,
                    )
                        .map { pagingData ->
                            pagingData.map { albumPhotoListing ->
                                PhotosItem.PhotoListing(
                                    albumPhotoListing.linkId,
                                    albumPhotoListing.captureTime,
                                    null
                                )
                            }
                        }
                )
            }.cachedIn(viewModelScope)

    fun viewEvent(
        navigateBack: () -> Unit,
    ) : AlbumViewEvent = object : AlbumViewEvent {
        override val onBackPressed = { navigateBack() }
        override val onLoadState: (CombinedLoadStates, Int) -> Unit = onLoadState(
            appContext = appContext,
            useExceptionMessage = configurationProvider.useExceptionMessage,
            listContentState = listContentState,
            listAppendContentState = listContentAppendingState,
            coroutineScope = viewModelScope,
            emptyState = MutableStateFlow(
                ListContentState.Empty(
                    imageResId = BasePresentation.drawable.empty_folder_dark, //TODO: replace with proper illustration once available
                    titleId = I18N.string.albums_new_album_name_hint, //TODO: replace with proper text once available
                )
            ),
        ) { message ->
            viewModelScope.launch {
                //TODO: we need a snackbar
            }
        }
        override val onScroll = this@AlbumViewModel::onScroll
        override val onErrorAction = { retry() }
        override val onRefresh = this@AlbumViewModel::onRefresh
    }

    private fun onScroll(driveLinkIds: Set<LinkId>) {
        if (driveLinkIds.isNotEmpty()) {
            fetchingJob?.cancel()
            fetchingJob = viewModelScope.launch {
                delay(300.milliseconds)
                photoDriveLinks.load(
                    (driveLinkIds + driveLink.value?.coverLinkId)
                        .filterNotNull()
                        .toSet()
                )
            }
        }
    }

    private fun retry() {
        viewModelScope.launch {
            if (driveLink.value == null) {
                retryDriveLink()
            } else {
                retryList()
            }
        }
    }

    private suspend fun retryDriveLink() {
        retryTrigger.emit(Unit)
        listContentState.value = ListContentState.Loading
    }

    private suspend fun retryList() {
        _listEffect.emit(ListEffect.RETRY)
    }

    private fun onRefresh() {
        viewModelScope.launch {
            _listEffect.emit(ListEffect.REFRESH)
        }
    }

    companion object {
        const val SHARE_ID = "shareId"
        const val ALBUM_ID = "albumId"
    }
}
