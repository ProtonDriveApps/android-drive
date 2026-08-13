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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.options.InvitationOption
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log as logResult
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.shared.domain.usecase.CanManageSharing
import me.proton.core.drive.drivelink.shared.presentation.entry.ShareUserOptionEntry
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ShareInvitationOptionsViewModel(
    @param:ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDriveLink,
    private val canManageSharing: CanManageSharing,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    protected val invitationId: String = savedStateHandle.require(KEY_INVITATION_ID)

    protected val linkId = FileId(
        ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)),
        savedStateHandle.require(KEY_LINK_ID)
    )

    protected val driveLink: StateFlow<DriveLink?> =
        getDriveLink(linkId = linkId).mapSuccessValueOrNull()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    abstract val invitation: Flow<ShareUser>

    abstract val options: Set<InvitationOption>

    fun entries(
        runAction: RunAction,
    ): Flow<List<ShareUserOptionEntry>> = combine(
        driveLink.filterNotNull(),
        invitation,
        canManageSharing(linkId),
    ) { driveLink, invitation, canManageSharing ->
        options.filter { option ->
            when (option) {
                InvitationOption.CopyInvitationLink -> true
                InvitationOption.PermissionsEditor,
                InvitationOption.PermissionsViewer,
                InvitationOption.ResendInvitation,
                InvitationOption.RemoveAccess -> canManageSharing
            }
        }.map { option ->
            when (option) {
                is InvitationOption.PermissionsViewer -> option.build(
                    isSelected = invitation.permissions == Permissions.viewer,
                    runAction = runAction,
                ) {
                    viewModelScope.launch {
                        updatePermissions(driveLink, Permissions.viewer)
                    }
                }

                is InvitationOption.PermissionsEditor -> option.build(
                    isSelected = invitation.permissions == Permissions.editor,
                    runAction = runAction,
                ) {
                    viewModelScope.launch {
                        updatePermissions(driveLink, Permissions.editor)
                    }
                }

                is InvitationOption.CopyInvitationLink -> option.build(runAction) {
                    viewModelScope.launch {
                        copyInvitationUrl(driveLink)
                    }
                }

                is InvitationOption.ResendInvitation -> option.build(runAction) {
                    viewModelScope.launch {
                        resendInvitation(driveLink)
                    }
                }

                is InvitationOption.RemoveAccess -> option.build(runAction) {
                    viewModelScope.launch {
                        deleteInvitation(driveLink)
                    }
                }
            }
        }
    }

    protected fun logAndBroadcastMessage(
        error: DataResult.Error,
        type: BroadcastMessage.Type = BroadcastMessage.Type.WARNING
    ) {
        error.logResult(VIEW_MODEL)
        broadcastMessages(
            userId = userId,
            message = error.getDefaultMessage(
                context = appContext,
                useExceptionMessage = configurationProvider.useExceptionMessage
            ),
            type = type,
        )
    }

    abstract suspend fun updatePermissions(driveLink: DriveLink, permissions: Permissions)

    abstract suspend fun deleteInvitation(driveLink: DriveLink)

    abstract suspend fun resendInvitation(driveLink: DriveLink)
    abstract suspend fun copyInvitationUrl(driveLink: DriveLink)

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_LINK_ID = "linkId"
        const val KEY_INVITATION_ID = "invitationId"
    }
}
