/*
 * Copyright (c) 2023 Proton AG.
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

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.usecase.AddToAlbumInfo
import me.proton.android.drive.ui.options.Option
import me.proton.android.drive.ui.options.OptionsFilter
import me.proton.android.drive.ui.options.filter
import me.proton.android.drive.ui.options.filterAlbums
import me.proton.android.drive.ui.options.filterAll
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.documentsprovider.domain.usecase.ExportToDownload
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isPhoto
import me.proton.core.drive.drivelink.domain.extension.toVolumePhotoListing
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAlbumsDisabled
import me.proton.core.drive.feature.flag.domain.usecase.AlbumsFeatureFlag
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.trash.domain.usecase.SendToTrash
import javax.inject.Inject

@HiltViewModel
class MultipleFileOrFolderOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getSelectedDriveLinks: GetSelectedDriveLinks,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    albumsFeatureFlag: AlbumsFeatureFlag,
    configurationProvider: ConfigurationProvider,
    private val sendToTrash: SendToTrash,
    private val exportToDownload: ExportToDownload,
    private val deselectLinks: DeselectLinks,
    private val addToAlbumInfo: AddToAlbumInfo,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val selectionId = SelectionId(requireNotNull(savedStateHandle.get(KEY_SELECTION_ID)))
    val selectedDriveLinks: Flow<List<DriveLink>> = getSelectedDriveLinks(selectionId)
    // Send -> ACTION_SEND_MULTIPLE (mime type aggregation) - we need to update sendfiledialog with multiple file download
    //   Mime type aggregation - all the same use that one, all same prefix use prefix/*  else use */*
    private val optionsFilter = savedStateHandle.require<OptionsFilter>(OPTIONS_FILTER)
    private val albumsFeature = albumsFeatureFlag(userId)
        .stateIn(viewModelScope, Eagerly, configurationProvider.albumsFeatureFlag)
    private val albumsKillSwitch = getFeatureFlagFlow(driveAlbumsDisabled(userId))
        .stateIn(viewModelScope, Eagerly, FeatureFlag(driveAlbumsDisabled(userId), NOT_FOUND))

    fun entries(
        driveLinks: List<DriveLink>,
        runAction: (suspend () -> Unit) -> Unit,
        navigateToMove: (SelectionId, parentId: FolderId?) -> Unit,
        navigateToCreateNewAlbum: () -> Unit,
        dismiss: () -> Unit,
    ): Flow<List<OptionEntry<Unit>>> = combine(
        albumsFeature,
        albumsKillSwitch,
    ) { albumsFeatureFlagOn, albumsKillSwitch ->
        options
            .filterAll(driveLinks)
            .filter(optionsFilter)
            .filterAlbums(albumsFeatureFlagOn, albumsKillSwitch)
            .map { option ->
                when (option) {
                    is Option.Trash -> option.build(
                        runAction = runAction,
                        moveToTrash = {
                            viewModelScope.launch {
                                sendToTrash(userId, driveLinks)
                                deselectLinks(selectionId)
                            }
                        },
                    )
                    is Option.Move -> option.build(
                        runAction = runAction,
                        navigateToMoveAll = {
                            navigateToMove(selectionId, driveLinks.first().parentId)
                        }
                    )
                    is Option.Download -> option.build(
                        runAction = runAction,
                        download = {
                            viewModelScope.launch {
                                exportToDownload(
                                    driveLinks.filterIsInstance<DriveLink.File>().map { driveLink -> driveLink.id }
                                )
                                deselectLinks(selectionId)
                            }
                        }
                    )
                    is Option.CreateAlbum -> option.build(
                        runAction = runAction,
                        createAlbum = {
                            viewModelScope.launch {
                                addToAlbumInfo(
                                    driveLinks
                                        .filterIsInstance<DriveLink.File>()
                                        .filter { driveLink -> driveLink.isPhoto }
                                        .map { driveLink -> driveLink.toVolumePhotoListing() }
                                        .toSet()
                                ).getOrNull(LogTag.PHOTO, "Adding photos to album info failed.")
                                navigateToCreateNewAlbum()
                                deselectLinks(selectionId)
                            }
                        },
                    )
                    else -> throw IllegalStateException(
                        "Option ${option.javaClass.simpleName} is not found. Did you forget to add it?"
                    )
                }
            }
            .apply {
                if (isEmpty() || driveLinks.isEmpty()) dismiss()
            }
    }

    companion object {
        const val KEY_SELECTION_ID = "selectionId"
        const val OPTIONS_FILTER = "optionsFilter"

        private val options = setOfNotNull(
            Option.CreateAlbum,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Option.Download else null,
            Option.Move,
            Option.Trash,
        )
    }
}
