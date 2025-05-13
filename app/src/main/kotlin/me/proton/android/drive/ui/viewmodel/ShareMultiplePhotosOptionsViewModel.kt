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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.android.drive.photos.domain.usecase.AddPhotosToAlbum
import me.proton.android.drive.photos.domain.usecase.AddToAlbumInfo
import me.proton.android.drive.photos.presentation.extension.processAdd
import me.proton.android.drive.photos.presentation.state.AlbumsItem
import me.proton.android.drive.ui.viewevent.ShareMultiplePhotosOptionsViewEvent
import me.proton.android.drive.ui.viewstate.ShareMultiplePhotosOptionsViewState
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.toVolumePhotoListing
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoDriveLinks
import me.proton.core.drive.drivelink.photo.domain.usecase.GetAllAlbumListings
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.domain.entity.Share
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@HiltViewModel
class ShareMultiplePhotosOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getSelectedDriveLinks: GetSelectedDriveLinks,
    getPhotoShare: GetPhotoShare,
    @ApplicationContext private val appContext: Context,
    private val getAllAlbumListings: GetAllAlbumListings,
    private val photoDriveLinks: PhotoDriveLinks,
    private val deselectLinks: DeselectLinks,
    private val addToAlbumInfo: AddToAlbumInfo,
    private val addPhotosToAlbum: AddPhotosToAlbum,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    val selectionId = SelectionId(savedStateHandle.require(SELECTION_ID))
    private val selectedDriveLinks: Flow<List<DriveLink>> = getSelectedDriveLinks(selectionId)
    private var fetchingJob: Job? = null
    private var runAction: RunAction? = null
    private var navigateToCreateNewAlbum: (() -> Unit)? = null
    private var navigateToAlbum: ((AlbumId) -> Unit)? = null
    private val driveLinksMap: StateFlow<Map<LinkId, DriveLink>> =
        photoDriveLinks.getDriveLinksMapFlow(userId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    private val photoShare: StateFlow<Share?> = getPhotoShare(userId)
        .filterSuccessOrError()
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val allAlbumListings: StateFlow<List<AlbumListing>> = photoShare
        .filterNotNull()
        .distinctUntilChanged()
        .transform { photoShare ->
            emitAll(
                getAllAlbumListings(
                    userId = userId,
                    volumeId = photoShare.volumeId,
                    filterBy = flowOf(AlbumListing.Filter.SHARED_BY_ME),
                )
            )
        }
        .transform { result ->
            if (result is DataResult.Success) {
                emit(result.value)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val sharedAlbums: Flow<List<AlbumsItem.Listing>> = combine(
        allAlbumListings,
        driveLinksMap,
    ) { listings, links ->
        listings.map { albumListing ->
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
                albumDetails = (links[albumListing.albumId] as? DriveLink.Album).details(),
            )
        }
    }

    val initialViewState: ShareMultiplePhotosOptionsViewState =
        ShareMultiplePhotosOptionsViewState(
            shareOptionsSectionTitleResId = I18N.string.albums_share_multiple_photos_options_section_title,
            shareOptions = listOf(
                object : OptionEntry<Unit> {
                    override val icon = CorePresentation.drawable.ic_proton_users
                    override val label = I18N.string.albums_share_multiple_photos_options_new_shared_album
                    override val onClick = { _: Unit ->
                        runAction?.invoke { onCreateNewAlbum() }
                        Unit
                    }
                },
            ),
            sharedAlbumsSectionTitleResId = I18N.string.albums_share_multiple_photos_options_shared_albums_section_title,
            sharedAlbums = sharedAlbums,
        )

    fun viewEvent(
        runAction: RunAction,
        navigateToCreateNewAlbum: () -> Unit,
        navigateToAlbum: (AlbumId) -> Unit,
    ): ShareMultiplePhotosOptionsViewEvent = object : ShareMultiplePhotosOptionsViewEvent {
        override val onScroll = this@ShareMultiplePhotosOptionsViewModel::onScroll
        override val onSharedAlbum = { albumId: AlbumId -> runAction { onSharedAlbum(albumId) } }
    }.also {
        this.runAction = runAction
        this.navigateToCreateNewAlbum = navigateToCreateNewAlbum
        this.navigateToAlbum = navigateToAlbum
    }

    private fun onScroll(driveLinkIds: Set<LinkId>) {
        if (driveLinkIds.isNotEmpty()) {
            fetchingJob?.cancel()
            fetchingJob = viewModelScope.launch {
                photoDriveLinks.load(driveLinkIds)
            }
        }
    }

    private fun onSharedAlbum(albumId: AlbumId) {
        viewModelScope.launch {
            selectedDriveLinks.firstOrNull()?.let { driveLinks ->
                driveLinks
                    .filterIsInstance<DriveLink.File>()
                    .map { photo -> photo.toVolumePhotoListing() }
                    .let { photoListings ->
                        addToAlbumInfo(albumId, photoListings.toSet())
                            .onFailure { error ->
                                error.log(VIEW_MODEL, "Failed to add photos to album info")
                                error.broadcast()
                            }
                            .onSuccess {
                                deselectLinks(selectionId)
                                showAddToAlbumStartMessage()
                                addPhotosToAlbum(albumId)
                                    .onFailure { error ->
                                        error.log(VIEW_MODEL, "Failed to add photos to album")
                                        error.broadcast()
                                    }
                                    .onSuccess { result ->
                                        result.processAdd(appContext) { message, type ->
                                            broadcastMessages(
                                                userId = userId,
                                                message = message,
                                                type = type,
                                            )
                                        }
                                        navigateToAlbum?.invoke(albumId)
                                    }
                            }
                    }
            }
        }
    }

    private fun onCreateNewAlbum() {
        viewModelScope.launch {
            selectedDriveLinks.firstOrNull()?.let { driveLinks ->
                driveLinks
                    .filterIsInstance<DriveLink.File>()
                    .map { photo -> photo.toVolumePhotoListing() }
                    .let { photoListings ->
                        addToAlbumInfo(photoListings.toSet())
                            .onFailure { error ->
                                error.log(VIEW_MODEL, "Failed to add photos to album info")
                                error.broadcast()
                            }
                            .onSuccess {
                                deselectLinks(selectionId)
                                navigateToCreateNewAlbum?.invoke()
                            }
                    }
            }
        }
    }

    private fun DriveLink.Album?.details(): String? = this?.let {
        appContext.getString(I18N.string.albums_share_multiple_photos_options_album_details)
    }

    private fun Throwable.broadcast() =
        broadcastMessages(
            userId = userId,
            message = getDefaultMessage(
                context = appContext,
                useExceptionMessage = configurationProvider.useExceptionMessage,
            ),
            type = BroadcastMessage.Type.ERROR,
        )

    private fun showAddToAlbumStartMessage() {
        broadcastMessages(
            userId = userId,
            message = appContext.getString(I18N.string.albums_add_to_album_start_message),
            type = BroadcastMessage.Type.INFO,
        )
    }

    companion object {
        const val SELECTION_ID = "selectionId"
    }
}
