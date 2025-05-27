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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.proton.android.drive.photos.domain.usecase.AddPhotosToAlbum
import me.proton.android.drive.photos.domain.usecase.AddToAlbumInfo
import me.proton.android.drive.ui.viewevent.ShareMultiplePhotosOptionsViewEvent
import me.proton.android.drive.ui.viewstate.ShareMultiplePhotosOptionsViewState
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.photo.domain.paging.PhotoDriveLinks
import me.proton.core.drive.drivelink.photo.domain.usecase.GetAllAlbumListings
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.photo.domain.entity.AlbumListing
import me.proton.core.drive.photo.domain.entity.PhotoListing
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
    @ApplicationContext appContext: Context,
    photoDriveLinks: PhotoDriveLinks,
    addToAlbumInfo: AddToAlbumInfo,
    addPhotosToAlbum: AddPhotosToAlbum,
    broadcastMessages: BroadcastMessages,
    configurationProvider: ConfigurationProvider,
    private val getAllAlbumListings: GetAllAlbumListings,
    private val deselectLinks: DeselectLinks,
) : MultiplePhotosOptionsViewModel(
    savedStateHandle = savedStateHandle,
    getPhotoShare = getPhotoShare,
    appContext = appContext,
    photoDriveLinks = photoDriveLinks,
    addToAlbumInfo = addToAlbumInfo,
    addPhotosToAlbum = addPhotosToAlbum,
    broadcastMessages = broadcastMessages,
    configurationProvider = configurationProvider,
) {
    val selectionId = SelectionId(savedStateHandle.require(SELECTION_ID))
    override val selectedDriveLinks: Flow<List<DriveLink>> = getSelectedDriveLinks(selectionId)
    override fun getAlbumListings(photoShare: Share): Flow<DataResult<List<AlbumListing>>> =
        getAllAlbumListings(
            userId = userId,
            volumeId = photoShare.volumeId,
            filterBy = flowOf(AlbumListing.Filter.SHARED_BY_ME),
        )

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
            sharedAlbums = albums,
        )

    fun viewEvent(
        runAction: RunAction,
        navigateToCreateNewAlbum: () -> Unit,
        navigateToAlbum: (AlbumId) -> Unit,
    ): ShareMultiplePhotosOptionsViewEvent = object : ShareMultiplePhotosOptionsViewEvent {
        override val onScroll = this@ShareMultiplePhotosOptionsViewModel::onScroll
        override val onSharedAlbum = { albumId: AlbumId -> runAction { onAlbum(albumId) } }
    }.also {
        this.runAction = runAction
        this.navigateToCreateNewAlbum = navigateToCreateNewAlbum
        this.navigateToAlbum = navigateToAlbum
    }


    private fun DriveLink.Album?.details(): String? = this?.let {
        appContext.getString(I18N.string.albums_share_multiple_photos_options_album_details)
    }

    override suspend fun onSuccessfullyAdded(photoListings: List<PhotoListing.Volume>) {
        deselectLinks(selectionId)
    }

    override fun getAlbumDetails(album: DriveLink.Album?): String? = album.details()

    companion object {
        const val SELECTION_ID = "selectionId"
    }
}
