/*
 * Copyright (c) 2023-2024 Proton AG.
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
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.photos.domain.usecase.AddPhotosToStream
import me.proton.android.drive.photos.domain.usecase.RemovePhotosFromAlbum
import me.proton.android.drive.photos.presentation.extension.processAddToStream
import me.proton.android.drive.photos.presentation.extension.processRemove
import me.proton.android.drive.ui.options.Option
import me.proton.android.drive.ui.options.filter
import me.proton.android.drive.ui.options.filterAlbums
import me.proton.android.drive.ui.options.filterPermissions
import me.proton.android.drive.ui.options.filterPhotoFavorite
import me.proton.android.drive.ui.options.filterProtonDocs
import me.proton.android.drive.ui.options.filterRoot
import me.proton.android.drive.ui.options.filterShare
import me.proton.android.drive.ui.options.filterShareMember
import me.proton.android.drive.usecase.LeaveShare
import me.proton.android.drive.usecase.NotifyActivityNotFound
import me.proton.android.drive.usecase.OpenProtonDocumentInBrowser
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.combine
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.documentsprovider.domain.usecase.ExportTo
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isShareMember
import me.proton.core.drive.drivelink.offline.domain.usecase.ToggleOffline
import me.proton.core.drive.drivelink.photo.domain.usecase.ToggleFavorite
import me.proton.core.drive.drivelink.photo.domain.usecase.UpdateAlbumCover
import me.proton.core.drive.drivelink.trash.domain.usecase.ToggleTrashState
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAlbumsDisabled
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAlbumsTempDisabledOnRelease
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveDocsDisabled
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingDevelopment
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.selection.domain.entity.SelectionId
import me.proton.core.drive.link.selection.domain.usecase.DeselectLinks
import me.proton.core.drive.link.selection.domain.usecase.SelectLinks
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.usecase.GetOldestActiveVolume
import me.proton.core.drive.volume.domain.usecase.HasPhotoVolume
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class FileOrFolderOptionsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDecryptedDriveLink,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    getOldestActiveVolume: GetOldestActiveVolume,
    private val toggleOffline: ToggleOffline,
    private val toggleTrashState: ToggleTrashState,
    private val exportTo: ExportTo,
    private val notifyActivityNotFound: NotifyActivityNotFound,
    private val leaveShare: LeaveShare,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
    private val openProtonDocumentInBrowser: OpenProtonDocumentInBrowser,
    private val updateAlbumCover: UpdateAlbumCover,
    private val removePhotosFromAlbum: RemovePhotosFromAlbum,
    private val deselectLinks: DeselectLinks,
    private val toggleFavorite: ToggleFavorite,
    private val addPhotosToStream: AddPhotosToStream,
    private val hasPhotoVolume: HasPhotoVolume,
    private val selectLinks: SelectLinks,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val selectionId = savedStateHandle.get<String?>(KEY_SELECTION_ID)?.let { SelectionId(it) }
    private var dismiss: (() -> Unit)? = null
    private val linkId: LinkId = FileId(
        ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)),
        savedStateHandle.require(KEY_LINK_ID)
    )
    private val albumId: AlbumId? = savedStateHandle.get<String>(KEY_ALBUM_ID)?.let { albumId ->
        AlbumId(
            ShareId(userId, savedStateHandle.require(KEY_ALBUM_SHARE_ID)),
            albumId
        )
    }
    val driveLink: Flow<DriveLink?> = getDriveLink(
        linkId = linkId,
        failOnDecryptionError = false,
    )
        .mapSuccessValueOrNull()
        .mapWithPrevious { previous, new ->
            if (previous != null && new == null) {
                dismiss?.invoke()
            }
            new
        }
        .stateIn(viewModelScope, Eagerly, null)

    private val sharingDevelopment = getFeatureFlagFlow(driveSharingDevelopment(userId))
        .stateIn(viewModelScope, Eagerly, FeatureFlag(driveSharingDevelopment(userId), NOT_FOUND))

    private val docsKillSwitch = getFeatureFlagFlow(driveDocsDisabled(userId))
        .stateIn(viewModelScope, Eagerly, FeatureFlag(driveDocsDisabled(userId), NOT_FOUND))

    private val albumsKillSwitch = getFeatureFlagFlow(driveAlbumsDisabled(userId))
        .stateIn(viewModelScope, Eagerly, FeatureFlag(driveAlbumsDisabled(userId), NOT_FOUND))

    private val shareTempDisabled = getFeatureFlagFlow(driveAlbumsTempDisabledOnRelease(userId))
        .stateIn(viewModelScope, Eagerly, FeatureFlag(driveAlbumsTempDisabledOnRelease(userId), NOT_FOUND))

    private val photoVolume = getOldestActiveVolume(userId, Volume.Type.PHOTO)
        .mapSuccessValueOrNull()
        .stateIn(viewModelScope, Eagerly, null)


    fun <T : DriveLink> entries(
        runAction: (suspend () -> Unit) -> Unit,
        navigateToInfo: (linkId: LinkId) -> Unit,
        navigateToMove: (linkId: LinkId, parentId: FolderId?) -> Unit,
        navigateToRename: (linkId: LinkId) -> Unit,
        navigateToDelete: (linkId: LinkId) -> Unit,
        navigateToSendFile: (fileId: FileId) -> Unit,
        navigateToManageAccess: (linkId: LinkId) -> Unit,
        navigateToShareViaInvitations: (linkId: LinkId) -> Unit,
        navigateToAddToAlbumsOptions: (selectionId: SelectionId) -> Unit,
        dismiss: () -> Unit,
        showCreateDocumentPicker: (String, () -> Unit) -> Unit = { _, _ -> },
    ): Flow<List<FileOptionEntry<T>>> = combine(
        this.driveLink.filterNotNull(),
        sharingDevelopment,
        docsKillSwitch,
        hasPhotoVolume(userId),
        albumsKillSwitch,
        shareTempDisabled,
    ) { driveLink, sharingDevelopment, protonDocsKillSwitch, hasPhotoVolume, albumsKillSwitch, shareTempDisabled ->
        options
            .filter(driveLink)
            .filterAlbums(hasPhotoVolume, albumsKillSwitch, albumId)
            .filterPhotoFavorite(hasPhotoVolume, albumsKillSwitch)
            .filterRoot(driveLink, sharingDevelopment)
            .filterShare(shareTempDisabled.on, albumId)
            .filterShareMember(driveLink.isShareMember)
            .filterPermissions(driveLink.sharePermissions ?: Permissions.owner)
            .filterProtonDocs(protonDocsKillSwitch)
            .map { option ->
                when (option) {
                    is Option.DeletePermanently -> option.build(runAction, navigateToDelete)
                    is Option.FavoriteToggle -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            toggleFavoriteAction(driveLink)
                            deselectLinks()
                        }
                    }
                    is Option.Info -> option.build(runAction) { linkId ->
                        navigateToInfo(linkId)
                        deselectLinks()
                    }
                    is Option.Move -> option.build(runAction) { linkId, parentId ->
                        navigateToMove(linkId, parentId)
                        deselectLinks()
                    }
                    is Option.OfflineToggle -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            toggleOffline(driveLink)
                            deselectLinks()
                        }
                    }
                    is Option.Rename -> option.build(runAction) { linkId ->
                        navigateToRename(linkId)
                        deselectLinks()
                    }
                    is Option.Trash -> option.build(
                        runAction = runAction,
                        toggleTrash = {
                            viewModelScope.launch {
                                toggleTrashState(driveLink)
                                deselectLinks()
                            }
                        }
                    )
                    is Option.SendFile -> option.build(runAction) { linkId ->
                        navigateToSendFile(linkId)
                        deselectLinks()
                    }
                    is Option.Download -> option.build { filename ->
                        showCreateDocumentPicker(filename) { handleActivityNotFound() }
                        deselectLinks()
                    }
                    is Option.ManageAccess -> option.build(runAction) { linkId ->
                        navigateToManageAccess(linkId)
                        deselectLinks()
                    }
                    is Option.ShareViaInvitations -> option.build(runAction) { linkId ->
                        navigateToShareViaInvitations(linkId)
                        deselectLinks()
                    }
                    is Option.RemoveMe -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            leaveShare(driveLink)
                            deselectLinks()
                        }
                    }
                    is Option.OpenInBrowser -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            openProtonDocumentInBrowser(driveLink)
                            deselectLinks()
                        }
                    }
                    is Option.SetAsAlbumCover -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            setAsAlbumCover(driveLink)
                            deselectLinks()
                        }
                    }
                    is Option.SaveSharePhoto -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            saveSharedPhoto(driveLink)
                            deselectLinks()
                        }
                    }
                    is Option.RemoveFromAlbum -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            removePhotosFromAlbum(driveLink)
                            deselectLinks()
                        }
                    }
                    is Option.AddToAlbums -> option.build(runAction) { driveLink ->
                            if (selectionId != null) {
                                navigateToAddToAlbumsOptions(selectionId)
                            } else {
                                viewModelScope.launch {
                                    selectLinks(listOf(driveLink.id))
                                        .onFailure { error ->
                                            error.log(VIEW_MODEL, "Failed to select links")
                                        }
                                        .onSuccess { selectionId ->
                                            navigateToAddToAlbumsOptions(selectionId)
                                        }
                                }
                            }
                    }
                    else -> throw IllegalStateException(
                        "Option ${option.javaClass.simpleName} is not found. Did you forget to add it?"
                    )
                }
            }.also {
                this.dismiss = dismiss
            }
    }

    private fun deselectLinks() {
        viewModelScope.launch {
            if (selectionId != null) {
                deselectLinks(selectionId)
            }
        }
    }

    private suspend fun removePhotosFromAlbum(
        driveLink: DriveLink.File,
    ) {
        removePhotosFromAlbum(
            albumId = requireNotNull(albumId),
            fileIds = listOf(driveLink.id),
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

    private suspend fun setAsAlbumCover(
        driveLink: DriveLink.File
    ) {
        updateAlbumCover(
            volumeId = driveLink.volumeId,
            albumId = requireNotNull(albumId),
            newCoverFileId = driveLink.id
        ).onFailure { error ->
            error.log(LogTag.ALBUM, "Cannot update album cover: ${driveLink.id.id.logId()}")
            broadcastMessages(
                userId = userId,
                message = error.getDefaultMessage(
                    appContext,
                    configurationProvider.useExceptionMessage
                ),
                type = BroadcastMessage.Type.ERROR
            )
        }.onSuccess {
            broadcastMessages(
                userId = userId,
                message = appContext.getString(I18N.string.albums_set_album_as_cover_success),
                type = BroadcastMessage.Type.INFO,
            )
        }
    }

    private suspend fun saveSharedPhoto(
        driveLink: DriveLink.File
    ) {
        val fileId = driveLink.id
        addPhotosToStream(
            photoIds = listOf(fileId),
            albumId = requireNotNull(albumId) { "album id is required to save shared photo"},
        ).onFailure { error ->
            error.log(LogTag.ALBUM, "Cannot copy photo to stream: ${fileId.id.logId()}")
            broadcastMessages(
                userId = userId,
                message = error.getDefaultMessage(
                    appContext,
                    configurationProvider.useExceptionMessage
                ),
                type = BroadcastMessage.Type.ERROR
            )
        }.onSuccess { result ->
            result.processAddToStream(appContext) { message, type ->
                broadcastMessages(
                    userId = userId,
                    message = message,
                    type = type,
                )
            }
        }
    }

    private suspend fun toggleFavoriteAction(
        driveLink: DriveLink.File
    ) {
        toggleFavorite(driveLink).onFailure { error ->
            error.log(VIEW_MODEL, "Cannot toggle favorite")
            broadcastMessages(
                userId = userId,
                message = error.getDefaultMessage(
                    appContext,
                    configurationProvider.useExceptionMessage
                ),
                type = BroadcastMessage.Type.ERROR
            )
        }.onSuccess {
            broadcastMessages(
                userId = userId,
                message = if(driveLink.isFavorite) {
                    appContext.getString(I18N.string.files_remove_from_favorite_action_success)
                } else {
                    appContext.getString(
                        if (driveLink.volumeId != photoVolume.value?.id) {
                            I18N.string.files_add_to_favorite_from_foreign_volume_action_success
                        } else {
                            I18N.string.files_add_to_favorite_action_success
                        }
                    )
                },
                type = BroadcastMessage.Type.INFO
            )
        }
    }

    fun onCreateDocumentResult(fileId: FileId, documentUri: Uri) {
        exportTo(fileId = fileId, destinationUri = documentUri)
        dismiss?.invoke()
    }

    private fun handleActivityNotFound() {
        this.dismiss?.invoke()
        notifyActivityNotFound(userId, I18N.string.operation_create_document)
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_LINK_ID = "linkId"
        const val KEY_ALBUM_ID = "albumId"
        const val KEY_ALBUM_SHARE_ID = "albumShareId"
        const val KEY_SELECTION_ID = "selectionId"

        private val options = setOf(
            Option.SetAsAlbumCover,
            Option.RemoveFromAlbum,
            Option.OfflineToggle,
            Option.FavoriteToggle,
            Option.AddToAlbums,
            Option.SaveSharePhoto,
            Option.ShareViaInvitations,
            Option.ManageAccess,
            Option.SendFile,
            Option.Download,
            Option.Move,
            Option.Rename,
            Option.OpenInBrowser,
            Option.Info,
            Option.Trash,
            Option.DeletePermanently,
            Option.RemoveMe,
        )
    }
}
