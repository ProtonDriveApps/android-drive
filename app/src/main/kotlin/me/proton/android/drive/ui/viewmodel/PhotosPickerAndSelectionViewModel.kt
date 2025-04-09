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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.usecase.AddToAlbumInfo
import me.proton.android.drive.photos.domain.usecase.GetAddToAlbumPhotoListings
import me.proton.android.drive.photos.domain.usecase.GetPhotoListingCount
import me.proton.android.drive.photos.domain.usecase.RemoveFromAlbumInfo
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.toVolumePhotoListing
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.drivelink.selection.domain.usecase.SelectAll
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import me.proton.core.drive.share.domain.entity.ShareId

open class PhotosPickerAndSelectionViewModel(
    savedStateHandle: SavedStateHandle,
    selectLinks: SelectLinks,
    deselectLinks: DeselectLinks,
    selectAll: SelectAll,
    getSelectedDriveLinks: GetSelectedDriveLinks,
    private val getPhotoListingCount: GetPhotoListingCount,
    private val addToAlbumInfo: AddToAlbumInfo,
    private val removeFromAlbumInfo: RemoveFromAlbumInfo,
    private val getAddToAlbumPhotoListings: GetAddToAlbumPhotoListings,
) : SelectionViewModel(
    savedStateHandle = savedStateHandle,
    selectLinks = selectLinks,
    deselectLinks = deselectLinks,
    selectAll = selectAll,
    getSelectedDriveLinks = getSelectedDriveLinks,
) {
    private val destinationAlbumId: AlbumId? = savedStateHandle.get<String?>(DESTINATION_SHARE_ID)
        ?.let { destinationShareId ->
            savedStateHandle.get<String?>(DESTINATION_ALBUM_ID)?.let { destinationAlbumId ->
                AlbumId(ShareId(userId, destinationShareId), destinationAlbumId)
            }
        }
    protected val inPickerMode: Boolean = savedStateHandle[IN_PICKER_MODE] ?: false

    override fun onDriveLink(driveLink: DriveLink, nonSelectedBlock: () -> Unit) {
        if (inPickerMode && driveLink is DriveLink.File) {
            if (selected.value.contains(driveLink.id)) {
                removeFromAlbum(driveLink)
                removeSelected(listOf(driveLink.id))
            } else {
                addToAlbum(driveLink)
                addSelected(listOf(driveLink.id))
            }
        } else {
            super.onDriveLink(driveLink, nonSelectedBlock)
        }
    }

    suspend fun initializeSelectionInPickerMode() {
        if (inPickerMode) {
            getAddToAlbumPhotoListings(userId, destinationAlbumId)
                .getOrNull(VIEW_MODEL, "Failed to get add to album photo listings")
                ?.map { photoListing ->
                    photoListing.linkId
                }
                ?.let { photoIds ->
                    if (photoIds.isNotEmpty()) {
                        removeAllSelected()
                        addSelected(photoIds)
                    }
                }
            getPhotoListingCount(userId, destinationAlbumId)
                .onEach { count ->
                    if (count == 0) {
                        removeAllSelected()
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun addToAlbum(driveLink: DriveLink.File) = viewModelScope.launch {
        val photoListings = setOf(driveLink.toVolumePhotoListing())
        if (destinationAlbumId == null) {
            addToAlbumInfo(photoListings)
        } else {
            addToAlbumInfo(destinationAlbumId, photoListings)
        }
    }

    private fun removeFromAlbum(driveLink: DriveLink.File) = viewModelScope.launch {
        val photoListings = setOf(driveLink.toVolumePhotoListing())
        if (destinationAlbumId == null) {
            removeFromAlbumInfo(photoListings)
        } else {
            removeFromAlbumInfo(destinationAlbumId, photoListings)
        }
    }

    companion object {
        const val IN_PICKER_MODE = "inPickerMode"
        const val DESTINATION_SHARE_ID = "destinationShareId"
        const val DESTINATION_ALBUM_ID = "destinationAlbumId"
    }
}
