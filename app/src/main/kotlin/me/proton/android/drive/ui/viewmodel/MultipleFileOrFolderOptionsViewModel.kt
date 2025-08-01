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

import android.content.Context
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.photos.domain.usecase.RemovePhotosFromAlbum
import me.proton.android.drive.photos.presentation.extension.processRemove
import me.proton.android.drive.ui.options.Option
import me.proton.android.drive.ui.options.filterAlbums
import me.proton.android.drive.ui.options.filterAll
import me.proton.android.drive.ui.options.filterPermissions
import me.proton.android.drive.ui.options.filterPhotoTag
import me.proton.android.drive.ui.options.filterShare
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.documentsprovider.domain.usecase.ExportToDownload
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.lowestCommonPermissions
import me.proton.core.drive.drivelink.selection.domain.usecase.GetSelectedDriveLinks
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAlbumsDisabled
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.files.presentation.entry.OptionEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.requireFolderId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.photo.domain.usecase.ScanPhotoForTags
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.trash.domain.usecase.SendToTrash
import me.proton.core.drive.volume.domain.usecase.HasPhotoVolume
import javax.inject.Inject

@HiltViewModel
class MultipleFileOrFolderOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getSelectedDriveLinks: GetSelectedDriveLinks,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    @ApplicationContext private val appContext: Context,
    private val sendToTrash: SendToTrash,
    private val exportToDownload: ExportToDownload,
    private val deselectLinks: DeselectLinks,
    private val removePhotosFromAlbum: RemovePhotosFromAlbum,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
    private val hasPhotoVolume: HasPhotoVolume,
    private val scanPhotoForTags: ScanPhotoForTags,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val selectionId = SelectionId(requireNotNull(savedStateHandle.get(KEY_SELECTION_ID)))
    val selectedDriveLinks: Flow<List<DriveLink>> = getSelectedDriveLinks(selectionId)
    // Send -> ACTION_SEND_MULTIPLE (mime type aggregation) - we need to update sendfiledialog with multiple file download
    //   Mime type aggregation - all the same use that one, all same prefix use prefix/*  else use */*
    private val albumId: AlbumId? = savedStateHandle.get<String>(KEY_ALBUM_ID)?.let { albumId ->
        AlbumId(
            ShareId(userId, savedStateHandle.require(KEY_ALBUM_SHARE_ID)),
            albumId
        )
    }
    private val albumsKillSwitch = getFeatureFlagFlow(driveAlbumsDisabled(userId))
        .stateIn(viewModelScope, Eagerly, FeatureFlag(driveAlbumsDisabled(userId), NOT_FOUND))

    fun entries(
        driveLinks: List<DriveLink>,
        runAction: (suspend () -> Unit) -> Unit,
        navigateToMove: (SelectionId, parentId: FolderId?) -> Unit,
        navigateToAddToAlbumsOptions: (SelectionId) -> Unit,
        navigateToShareMultiplePhotosOptions: (SelectionId) -> Unit,
        dismiss: () -> Unit,
    ): Flow<List<OptionEntry<Unit>>> = combine(
        hasPhotoVolume(userId),
        albumsKillSwitch,
    ) { hasPhotoVolume, albumsKillSwitch ->
        options
            .filterAll(driveLinks)
            .filterAlbums(hasPhotoVolume, albumsKillSwitch, albumId)
            .filterPhotoTag(configurationProvider.scanPhotoFileForTags)
            .filterShare(false, albumId)
            .filterPermissions(driveLinks.lowestCommonPermissions)
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
                            navigateToMove(selectionId, driveLinks.first().requireFolderId())
                        }
                    )
                    is Option.TagPhotoFile -> option.build(
                        runAction = runAction,
                        tagPhotos = {
                            viewModelScope.launch {
                                scanPhotoForTags(driveLinks)
                                deselectLinks(selectionId)
                            }
                        }
                    )
                    is Option.Download -> option.build(
                        runAction = runAction,
                        download = {
                            viewModelScope.launch {
                                exportToDownload(
                                    driveLinks.filterIsInstance<DriveLink.File>().map { driveLink -> driveLink.id }
                                )
                            }
                        }
                    )
                    is Option.AddToAlbums -> option.build(
                        runAction = runAction,
                        navigateToAddToAlbumsOptions = {
                            navigateToAddToAlbumsOptions(selectionId)
                        }
                    )
                    is Option.RemoveFromAlbum -> option.build(
                        runAction = runAction,
                        removeSelectedFromAlbum = {
                            viewModelScope.launch {
                                removePhotosFromAlbum(
                                    driveLinks
                                        .filterIsInstance<DriveLink.File>()
                                )
                                deselectLinks(selectionId)
                            }
                        },
                    )
                    is Option.ShareMultiplePhotos -> option.build(
                        runAction = runAction,
                        navigateToShareMultiplePhotosOptions = {
                            navigateToShareMultiplePhotosOptions(selectionId)
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

    private suspend fun scanPhotoForTags(driveLinks: List<DriveLink>) {
        scanPhotoForTags(
            driveLinks.first().volumeId,
            driveLinks.map { it.link }.filterIsInstance<Link.File>()
        ).onFailure { error ->
            error.log(VIEW_MODEL, "Failed to scan ${driveLinks.size} photos for tags")
        }
    }

    private suspend fun removePhotosFromAlbum(
        driveLinks: List<DriveLink.File>,
    ) {
        removePhotosFromAlbum(
            albumId = requireNotNull(albumId),
            fileIds = driveLinks.map { driveLink -> driveLink.id },
        )
            .onFailure { error ->
                error.log(VIEW_MODEL, "Failed to remove file from album")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        appContext,
                        configurationProvider.useExceptionMessage,
                    ),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
            .onSuccess { result ->
                result.processRemove(appContext) { message, type ->
                    broadcastMessages(
                        userId = userId,
                        message = message,
                        type = type,
                    )
                }
            }
    }

    companion object {
        const val KEY_SELECTION_ID = "selectionId"
        const val KEY_ALBUM_ID = "albumId"
        const val KEY_ALBUM_SHARE_ID = "albumShareId"

        private val options = setOfNotNull(
            Option.TagPhotoFile,
            Option.RemoveFromAlbum,
            Option.AddToAlbums,
            Option.ShareMultiplePhotos,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Option.Download else null,
            Option.Move,
            Option.Trash,
        )
    }
}
