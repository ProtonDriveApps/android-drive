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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.options.Option
import me.proton.android.drive.ui.options.filter
import me.proton.android.drive.ui.options.filterPermissions
import me.proton.android.drive.ui.options.filterRoot
import me.proton.android.drive.ui.options.filterShare
import me.proton.android.drive.ui.options.filterShareMember
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.crypto.domain.usecase.GetDecryptedDriveLink
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.extension.isShareMember
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveAlbumsTempDisabledOnRelease
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingDevelopment
import me.proton.core.drive.feature.flag.domain.extension.on
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlagFlow
import me.proton.core.drive.files.presentation.entry.FileOptionEntry
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

@HiltViewModel
class AlbumOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDecryptedDriveLink,
    getFeatureFlagFlow: GetFeatureFlagFlow,
    private val broadcastMessages: BroadcastMessages,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {
    private val albumId = AlbumId(
        shareId = ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)),
        id = savedStateHandle.require(KEY_ALBUM_ID)
    )
    private val shareTempDisabled = getFeatureFlagFlow(driveAlbumsTempDisabledOnRelease(userId))
        .stateIn(viewModelScope, Eagerly, FeatureFlag(driveAlbumsTempDisabledOnRelease(userId), NOT_FOUND))
    private val sharingDevelopment = getFeatureFlagFlow(driveSharingDevelopment(userId))
        .stateIn(viewModelScope, Eagerly, FeatureFlag(driveSharingDevelopment(userId), NOT_FOUND))
    private var dismiss: (() -> Unit)? = null
    val driveLink: StateFlow<DriveLink.Album?> = getDriveLink(albumId = albumId)
        .mapSuccessValueOrNull()
        .mapWithPrevious { previous, new ->
            if (previous != null && new == null) {
                dismiss?.invoke()
            }
            new
        }
        .stateIn(viewModelScope, Eagerly, null)

    val coverLink: StateFlow<DriveLink.File?> = driveLink
        .filterNotNull()
        .distinctUntilChanged()
        .mapNotNull { album -> album.coverLinkId }
        .transform { coverLinkId ->
            emitAll(
                getDriveLink(fileId = coverLinkId)
                    .mapSuccessValueOrNull()
            )
        }
        .stateIn(viewModelScope, Eagerly, null)

    fun entries(
        runAction: RunAction,
        navigateToShareViaInvitations: (linkId: LinkId) -> Unit,
        navigateToManageAccess: (linkId: LinkId) -> Unit,
        navigateToRename: (linkId: LinkId) -> Unit,
        navigateToDelete: (AlbumId) -> Unit,
        navigateToLeave: (AlbumId) -> Unit,
        dismiss: () -> Unit,
    ): Flow<List<FileOptionEntry<DriveLink.Album>>> = combine(
        driveLink.filterNotNull(),
        shareTempDisabled,
        sharingDevelopment,
    ) { driveLink, shareTempDisabled, sharingDevelopment ->
        options
            .filter(driveLink)
            .filterShareMember(driveLink.isShareMember)
            .filterPermissions(driveLink.sharePermissions ?: Permissions.owner)
            .filterShare(shareTempDisabled.on)
            .filterRoot(driveLink, sharingDevelopment)
            .map { option ->
                when (option) {
                    is Option.OfflineToggle -> option.build(runAction) { driveLink ->
                        notYetImplemented()
                    }
                    is Option.ShareViaInvitations -> option.build(runAction, navigateToShareViaInvitations)
                    is Option.ManageAccess -> option.build(runAction, navigateToManageAccess)
                    is Option.Rename -> option.build(runAction, navigateToRename)
                    is Option.DeleteAlbum -> option.build(runAction = runAction) { albumId ->
                        navigateToDelete(albumId)
                    }
                    is Option.LeaveAlbum -> option.build(runAction) { album ->
                        navigateToLeave(album.id)
                    }
                    else -> error(
                        "Option ${option.javaClass.simpleName} is not found. Did you forget to add it?"
                    )
                }
            }.also {
                this.dismiss = dismiss
            }
    }

    private fun notYetImplemented() = viewModelScope.launch {
        broadcastMessages(
            userId = userId,
            message = "Not yet implemented",
            type = BroadcastMessage.Type.INFO,
        )
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_ALBUM_ID = "albumId"

        private val options = setOf(
            //Option.OfflineToggle,
            Option.ShareViaInvitations,
            Option.ManageAccess,
            Option.Rename,
            Option.DeleteAlbum,
            Option.LeaveAlbum,
        )
    }
}
