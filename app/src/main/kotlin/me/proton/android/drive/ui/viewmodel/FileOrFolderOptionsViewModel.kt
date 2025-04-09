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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.extension.getDefaultMessage
import me.proton.android.drive.photos.domain.usecase.RemovePhotosFromAlbum
import me.proton.android.drive.photos.presentation.extension.processRemove
import me.proton.android.drive.ui.options.Option
import me.proton.android.drive.ui.options.filter
import me.proton.android.drive.ui.options.filterAlbums
import me.proton.android.drive.ui.options.filterPermissions
import me.proton.android.drive.ui.options.filterProtonDocs
import me.proton.android.drive.ui.options.filterRoot
import me.proton.android.drive.ui.options.filterShareMember
import me.proton.android.drive.usecase.NotifyActivityNotFound
import me.proton.android.drive.usecase.OpenProtonDocumentInBrowser
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.extension.onFailure
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
import me.proton.core.drive.drivelink.photo.domain.usecase.UpdateAlbumCover
import me.proton.core.drive.drivelink.shared.domain.extension.sharingDetails
import me.proton.core.drive.drivelink.trash.domain.usecase.ToggleTrashState
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAlbumsDisabled
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveDocsDisabled
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingDevelopment
import me.proton.core.drive.feature.flag.domain.usecase.AlbumsFeatureFlag
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.usecase.LeaveShare
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class FileOrFolderOptionsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDecryptedDriveLink,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    albumsFeatureFlag: AlbumsFeatureFlag,
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
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
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

    private val albumsFeature = albumsFeatureFlag(userId)
        .stateIn(viewModelScope, Eagerly, configurationProvider.albumsFeatureFlag)
    private val albumsKillSwitch = getFeatureFlagFlow(driveAlbumsDisabled(userId))
        .stateIn(viewModelScope, Eagerly, FeatureFlag(driveAlbumsDisabled(userId), NOT_FOUND))

    fun <T : DriveLink> entries(
        runAction: (suspend () -> Unit) -> Unit,
        navigateToInfo: (linkId: LinkId) -> Unit,
        navigateToMove: (linkId: LinkId, parentId: FolderId?) -> Unit,
        navigateToRename: (linkId: LinkId) -> Unit,
        navigateToDelete: (linkId: LinkId) -> Unit,
        navigateToSendFile: (fileId: FileId) -> Unit,
        navigateToManageAccess: (linkId: LinkId) -> Unit,
        navigateToShareViaInvitations: (linkId: LinkId) -> Unit,
        dismiss: () -> Unit,
        showCreateDocumentPicker: (String, () -> Unit) -> Unit = { _, _ -> },
    ): Flow<List<FileOptionEntry<T>>> = combine(
        this.driveLink.filterNotNull(),
        sharingDevelopment,
        docsKillSwitch,
        albumsFeature,
        albumsKillSwitch,
    ) { driveLink, sharingDevelopment, protonDocsKillSwitch, albumsFeatureFlagOn, albumsKillSwitch ->
        options
            .filter(driveLink)
            .filterAlbums(albumsFeatureFlagOn, albumsKillSwitch, albumId)
            .filterRoot(driveLink, sharingDevelopment)
            .filterShareMember(driveLink.isShareMember)
            .filterPermissions(driveLink.sharePermissions ?: Permissions.owner)
            .filterProtonDocs(protonDocsKillSwitch)
            .map { option ->
                when (option) {
                    is Option.DeletePermanently -> option.build(runAction, navigateToDelete)
                    is Option.Info -> option.build(runAction, navigateToInfo)
                    is Option.Move -> option.build(runAction, navigateToMove)
                    is Option.OfflineToggle -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            toggleOffline(driveLink)
                        }
                    }
                    is Option.Rename -> option.build(runAction, navigateToRename)
                    is Option.Trash -> option.build(
                        runAction = runAction,
                        toggleTrash = {
                            viewModelScope.launch {
                                toggleTrashState(driveLink)
                            }
                        }
                    )
                    is Option.SendFile -> option.build(runAction, navigateToSendFile)
                    is Option.Download -> option.build { filename ->
                        showCreateDocumentPicker(filename) { handleActivityNotFound() }
                    }
                    is Option.ManageAccess -> option.build(runAction, navigateToManageAccess)
                    is Option.ShareViaInvitations -> option.build(runAction, navigateToShareViaInvitations)
                    is Option.RemoveMe -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            leaveShare(driveLink)
                        }
                    }
                    is Option.OpenInBrowser -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            openProtonDocumentInBrowser(driveLink)
                        }
                    }
                    is Option.SetAsAlbumCover -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            setAsAlbumCover(driveLink)
                        }
                    }
                    is Option.RemoveFromAlbum -> option.build(runAction) { driveLink ->
                        viewModelScope.launch {
                            removePhotosFromAlbum(driveLink)
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

    private suspend fun leaveShare(driveLink: DriveLink) {
        val shareId = driveLink.sharingDetails?.shareId
        val memberId = driveLink.shareUser?.id
        if (shareId != null && memberId != null && shareId == driveLink.shareId) {
            leaveShare(driveLink.volumeId, driveLink.id, memberId).last().onFailure { error ->
                error.log(LogTag.SHARING, "Cannot leave share")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        appContext,
                        configurationProvider.useExceptionMessage
                    ),
                    type = BroadcastMessage.Type.ERROR
                )
            }
        } else {
            CoreLogger.i(
                tag = VIEW_MODEL,
                message = """
                    Skipping leave share (DriveLink.shareId=${driveLink.shareId.id.logId()},
                    SharingDetails.shareId=${shareId?.id?.logId()}, memberId=${memberId?.logId()})
                """.trimIndent(),
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

        private val options = setOf(
            Option.SetAsAlbumCover,
            Option.RemoveFromAlbum,
            Option.OfflineToggle,
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
