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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.usecase.AddToAlbumInfo
import me.proton.android.drive.photos.domain.usecase.GetAddToAlbumPhotoListings
import me.proton.android.drive.photos.domain.usecase.GetPhotoListingCount
import me.proton.android.drive.photos.domain.usecase.RemoveFromAlbumInfo
import me.proton.android.drive.photos.presentation.extension.details
import me.proton.android.drive.photos.presentation.state.PhotosItem
import me.proton.android.drive.photos.presentation.viewevent.AlbumViewEvent
import me.proton.android.drive.photos.presentation.viewstate.AlbumViewState
import me.proton.android.drive.ui.common.onClick
import me.proton.android.drive.usecase.OnFilesDriveLinkError
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.common.Action
import me.proton.core.drive.base.presentation.effect.ListEffect
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.state.ListContentAppendingState
import me.proton.core.drive.base.presentation.state.ListContentState
import me.proton.core.drive.base.presentation.viewmodel.onLoadState
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoDriveLinks
import me.proton.core.drive.drivelink.photo.domain.usecase.GetPagedAlbumPhotoListingsList
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.drivelink.selection.domain.usecase.SelectAll
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import me.proton.core.drive.base.presentation.R as BasePresentation
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
@ExperimentalCoroutinesApi
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDecryptedDriveLink: GetDecryptedDriveLink,
    selectLinks: SelectLinks,
    deselectLinks: DeselectLinks,
    selectAll: SelectAll,
    getSelectedDriveLinks: GetSelectedDriveLinks,
    getPhotoListingCount: GetPhotoListingCount,
    addToAlbumInfo: AddToAlbumInfo,
    removeFromAlbumInfo: RemoveFromAlbumInfo,
    getAddToAlbumPhotoListings: GetAddToAlbumPhotoListings,
    @ApplicationContext private val appContext: Context,
    private val onFilesDriveLinkError: OnFilesDriveLinkError,
    private val photoDriveLinks: PhotoDriveLinks,
    private val getPagedAlbumPhotoListingsList: GetPagedAlbumPhotoListingsList,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
) : PhotosPickerAndSelectionViewModel(
    savedStateHandle = savedStateHandle,
    selectLinks = selectLinks,
    deselectLinks = deselectLinks,
    selectAll = selectAll,
    getSelectedDriveLinks = getSelectedDriveLinks,
    getPhotoListingCount = getPhotoListingCount,
    addToAlbumInfo = addToAlbumInfo,
    removeFromAlbumInfo = removeFromAlbumInfo,
    getAddToAlbumPhotoListings = getAddToAlbumPhotoListings,
) {
    override val filterByParentId: Boolean = false
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
    private var viewEvent: AlbumViewEvent? = null
    private val albumOptionsAction = Action(
        iconResId = CorePresentation.drawable.ic_proton_three_dots_vertical,
        contentDescriptionResId = I18N.string.common_more,
        notificationDotVisible = false,
        onAction = { viewEvent?.onAlbumOptions?.invoke() },
    )
    private val topBarActions: MutableStateFlow<Set<Action>> = MutableStateFlow(setOf(albumOptionsAction))
    private val retryTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val driveLink: StateFlow<DriveLink.Album?> = retryTrigger.transformLatest {
        emitAll(
            getDecryptedDriveLink(albumId, failOnDecryptionError = false)
                .filterSuccessOrError()
                .mapWithPrevious { previous, result ->
                    result
                        .onSuccess { driveLink ->
                            CoreLogger.d(VIEW_MODEL, "drive link (${driveLink.id.id.logId()}) onSuccess")
                            driveLink.coverLinkId?.let { coverLinkId ->
                                photoDriveLinks.load(setOf(coverLinkId))
                            }
                            parentId.value = driveLink.id
                            return@mapWithPrevious driveLink
                        }
                        .onFailure { error ->
                            CoreLogger.d(VIEW_MODEL, "drive link (${albumId.id.logId()}) onFailure")
                            error.cause?.let { throwable ->
                                CoreLogger.d(VIEW_MODEL, throwable, "drive link (${albumId.id.logId()}) onFailure")
                                if (throwable is NoSuchElementException) {
                                    viewEvent?.onBackPressed?.invoke()
                                    return@onFailure
                                }
                            }
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
        selected,
    ) { album, contentState, selected ->
        topBarActions.value = when {
            inPickerMode -> emptySet()
            selected.isNotEmpty() -> setOf(
                selectedOptionsAction {
                    viewEvent?.onSelectedOptions?.invoke()
                }
            )
            else -> setOf(albumOptionsAction)
        }
        AlbumViewState(
            name = album.name,
            details = album.details(appContext),
            coverLinkId = album.coverLinkId,
            listContentState = contentState,
            isRefreshEnabled = contentState !is ListContentState.Loading,
            topBarActions = topBarActions,
            selected = this@AlbumViewModel.selected,
            inMultiselect = selected.isNotEmpty() || inPickerMode,
            showActions = !inPickerMode,
            navigationIconResId = if (selected.isEmpty() || inPickerMode) {
                CorePresentation.drawable.ic_arrow_back
            } else {
                CorePresentation.drawable.ic_proton_close
            },
            title = takeIf { selected.isNotEmpty() && !inPickerMode }
                ?.let {
                    appContext.quantityString(
                        I18N.plurals.common_selected,
                        selected.size
                    )
                },
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
        navigateToAlbumOptions: (AlbumId) -> Unit,
        navigateToPhotosOptions: (FileId, AlbumId?) -> Unit,
        navigateToMultiplePhotosOptions: (selectionId: SelectionId, AlbumId?) -> Unit,
        navigateToPreview: (FileId, AlbumId) -> Unit,
        navigateToPicker: (AlbumId) -> Unit,
        navigateBack: () -> Unit,
        lifecycle: Lifecycle,
    ) : AlbumViewEvent = object : AlbumViewEvent {
        private val driveLinkShareFlow =
            MutableSharedFlow<DriveLink>(extraBufferCapacity = 1).also { flow ->
                viewModelScope.launch {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        flow.take(1).collect { driveLink ->
                            driveLink.onClick(
                                navigateToFolder = { _, _ -> error("Photos should not have folders") },
                                navigateToPreview = { linkId ->
                                    viewModelScope.launch {
                                        navigateToPreview(
                                            linkId,
                                            this@AlbumViewModel.driveLink.filterNotNull().first().id,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }

        override val onTopAppBarNavigation = onTopAppBarNavigation {
            onBackPressed()
        }
        override val onBackPressed = { navigateBack() }
        override val onLoadState: (CombinedLoadStates, Int) -> Unit = onLoadState(
            appContext = appContext,
            useExceptionMessage = configurationProvider.useExceptionMessage,
            listContentState = listContentState,
            listAppendContentState = listContentAppendingState,
            coroutineScope = viewModelScope,
            emptyState = MutableStateFlow(
                ListContentState.Empty(
                    imageResId = BasePresentation.drawable.ic_proton_images_50,
                    titleId = I18N.string.albums_empty_album_screen_title,
                    descriptionResId = I18N.string.albums_empty_album_screen_description,
                )
            ),
        ) { message ->
            broadcastMessages(
                userId = userId,
                message = message,
                type = BroadcastMessage.Type.ERROR,
            )
        }
        override val onScroll = this@AlbumViewModel::onScroll
        override val onErrorAction = { retry() }
        override val onRefresh = this@AlbumViewModel::onRefresh
        override val onAlbumOptions = {
            viewModelScope.launch {
                navigateToAlbumOptions(driveLink.filterNotNull().first().id)
            }
            Unit
        }
        override val onDriveLink = { driveLink: DriveLink ->
            onDriveLink(driveLink) {
                driveLinkShareFlow.tryEmit(driveLink)
                Unit
            }
        }
        override val onSelectDriveLink = { driveLink: DriveLink ->
            onSelectDriveLink(driveLink)
        }
        override val onAddToAlbum = { onAddToAlbum(navigateToPicker) }
        override val onSelectedOptions =
            { onSelectedOptions(navigateToPhotosOptions, navigateToMultiplePhotosOptions, albumId) }
        override val onBack = { onBack() }
    }.also { viewEvent ->
        this.viewEvent = viewEvent
    }

    override fun onTopAppBarNavigation(nonSelectedBlock: () -> Unit): () -> Unit = {
        if (inPickerMode) {
            viewEvent?.onBackPressed?.invoke()
        } else {
            super.onTopAppBarNavigation(nonSelectedBlock).invoke()
        }
    }

    private fun onAddToAlbum(navigateToPicker: (AlbumId) -> Unit) {
        viewModelScope.launch {
            navigateToPicker(this@AlbumViewModel.driveLink.filterNotNull().first().id)
        }
    }

    private fun onScroll(driveLinkIds: Set<LinkId>) {
        if (driveLinkIds.isNotEmpty()) {
            fetchingJob?.cancel()
            fetchingJob = viewModelScope.launch {
                delay(100.milliseconds)
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
