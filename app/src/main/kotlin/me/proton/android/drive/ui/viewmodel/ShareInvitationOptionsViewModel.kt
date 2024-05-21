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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.options.InvitationOption
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.Permissions.Permission.READ
import me.proton.core.drive.base.domain.entity.Permissions.Permission.WRITE
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.SHARING
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.shared.domain.extension.permissions
import me.proton.core.drive.drivelink.shared.domain.extension.sharingDetails
import me.proton.core.drive.drivelink.shared.presentation.entry.ShareUserOptionEntry
import me.proton.core.drive.drivelink.shared.presentation.extension.toViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.usecase.CopyInvitationUrl
import me.proton.core.drive.share.user.domain.usecase.GetInvitationFlow
import me.proton.core.drive.share.user.domain.usecase.UpdateInvitationPermissions
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShareInvitationOptionsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDriveLink,
    getInvitationFlow: GetInvitationFlow,
    private val updateInvitationPermissions: UpdateInvitationPermissions,
    private val copyInvitationUrl: CopyInvitationUrl,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val invitationId: String = savedStateHandle.require(KEY_INVITATION_ID)

    private val linkId = FileId(
        ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)),
        savedStateHandle.require(KEY_LINK_ID)
    )

    private val viewerPermissions = Permissions().add(READ)
    private val editorPermissions = Permissions().add(READ).add(WRITE)

    private val driveLink: Flow<DriveLink?> = getDriveLink(linkId = linkId).mapSuccessValueOrNull()

    private val invitation = driveLink.filterNotNull().transformLatest { driveLink ->
        emitAll(
            getInvitationFlow(
                requireNotNull(driveLink.sharingDetails?.shareId) { "Sharing share id cannot be null" },
                invitationId
            )
        )
    }

    val viewState: Flow<ShareUserViewState> = invitation.map { invitee ->
        invitee.toViewState(appContext)
    }

    fun entries(
        runAction: RunAction,
    ): Flow<List<ShareUserOptionEntry>> = combine(
        driveLink.filterNotNull(),
        invitation,
    ) { driveLink, invitation ->
        options.filter { option ->
            when (option) {
                InvitationOption.CopyInvitationLink -> true
                InvitationOption.PermissionsEditor,
                InvitationOption.PermissionsViewer -> driveLink.permissions.canWrite
            }
        }.map { option ->
            when (option) {
                is InvitationOption.PermissionsViewer -> option.build(
                    isSelected = invitation.permissions == viewerPermissions,
                    runAction = runAction,
                ) {
                    viewModelScope.launch {
                        updatePermissions(driveLink, viewerPermissions) { runAction {} }
                    }
                }

                is InvitationOption.PermissionsEditor -> option.build(
                    isSelected = invitation.permissions == editorPermissions,
                    runAction = runAction,
                ) {
                    viewModelScope.launch {
                        updatePermissions(driveLink, editorPermissions) { runAction {} }
                    }
                }

                is InvitationOption.CopyInvitationLink -> option.build(runAction) {
                    viewModelScope.launch {
                        copyInvitationUrl(driveLink.volumeId, linkId, invitationId)
                    }
                }

                else -> error("Option ${option.javaClass.simpleName} is not found. Did you forget to add it?")
            }
        }
    }

    private suspend fun updatePermissions(
        driveLink: DriveLink,
        permissions: Permissions,
        onComplete: () -> Unit,
    ) {
        updateInvitationPermissions(
            shareId = requireNotNull(driveLink.sharingDetails?.shareId),
            invitationId = invitationId,
            permissions = permissions,
        ).collectLatest { dataResult ->
            dataResult.onFailure { error ->
                error.log(SHARING)
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(appContext, configurationProvider.useExceptionMessage),
                    type = BroadcastMessage.Type.WARNING,
                )
            }.onSuccess {
                onComplete()
            }
        }
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_LINK_ID = "linkId"
        const val KEY_INVITATION_ID = "invitationId"

        private val options = setOf(
            InvitationOption.PermissionsViewer,
            InvitationOption.PermissionsEditor,
            InvitationOption.CopyInvitationLink,
        )
    }
}
