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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.extension.log
import me.proton.android.drive.photos.domain.usecase.AddPhotosToAlbum
import me.proton.android.drive.photos.domain.usecase.AddToAlbumInfo
import me.proton.android.drive.photos.presentation.extension.processAdd
import me.proton.android.drive.photos.presentation.state.AlbumsItem
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.toVolumePhotoListing
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoDriveLinks
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.photo.domain.entity.PhotoListing
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.i18n.R as I18N

abstract class MultiplePhotosOptionsViewModel(
    savedStateHandle: SavedStateHandle,
    getPhotoShare: GetPhotoShare,
    protected val appContext: Context,
    private val photoDriveLinks: PhotoDriveLinks,
    private val addToAlbumInfo: AddToAlbumInfo,
    private val addPhotosToAlbum: AddPhotosToAlbum,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    protected abstract val selectedDriveLinks: Flow<List<DriveLink>>
    private var fetchingJob: Job? = null
    protected var runAction: RunAction? = null
    protected var navigateToCreateNewAlbum: (() -> Unit)? = null
    protected var navigateToAlbum: ((AlbumId) -> Unit)? = null
    private val driveLinksMap: StateFlow<Map<LinkId, DriveLink>> =
        photoDriveLinks.getDriveLinksMapFlow(userId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    protected val photoShare: StateFlow<Share?> = getPhotoShare(userId)
        .filterSuccessOrError()
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val albumListings: StateFlow<List<AlbumListing>> = photoShare
        .filterNotNull()
        .distinctUntilChanged()
        .transform { photoShare ->
            emitAll(
                getAlbumListings(photoShare)
            )
        }
        .transform { result ->
            if (result is DataResult.Success) {
                emit(result.value)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    protected val albums: Flow<List<AlbumsItem.Listing>> = combine(
        albumListings,
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
                albumDetails = getAlbumDetails(links[albumListing.albumId] as? DriveLink.Album),
            )
        }
    }

    protected fun onScroll(driveLinkIds: Set<LinkId>) {
        if (driveLinkIds.isNotEmpty()) {
            fetchingJob?.cancel()
            fetchingJob = viewModelScope.launch {
                photoDriveLinks.load(driveLinkIds)
            }
        }
    }

    protected fun onCreateNewAlbum() {
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
                                onSuccessfullyAdded(photoListings)
                                navigateToCreateNewAlbum?.invoke()
                            }
                    }
            }
        }
    }

    protected fun onAlbum(albumId: AlbumId) {
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
                                onSuccessfullyAdded(photoListings)
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

    protected abstract fun getAlbumDetails(album: DriveLink.Album?): String?

    protected abstract fun getAlbumListings(photoShare: Share): Flow<DataResult<List<AlbumListing>>>

    protected abstract suspend fun onSuccessfullyAdded(photoListings: List<PhotoListing.Volume>)

    protected fun Throwable.broadcast(type: BroadcastMessage.Type = BroadcastMessage.Type.ERROR) =
        broadcastMessages(
            userId = userId,
            message = getDefaultMessage(
                context = appContext,
                useExceptionMessage = configurationProvider.useExceptionMessage,
            ),
            type = type,
        )

    protected fun showAddToAlbumStartMessage() {
        broadcastMessages(
            userId = userId,
            message = appContext.getString(I18N.string.albums_add_to_album_start_message),
            type = BroadcastMessage.Type.INFO,
        )
    }
}
