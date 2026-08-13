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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import me.proton.android.drive.ui.options.MemberOption
import me.proton.core.compose.component.bottomsheet.RunAction
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.VIEW_MODEL
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.usecase.GetUserEmailsFlow
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.shared.domain.extension.sharingDetails
import me.proton.core.drive.drivelink.shared.domain.usecase.CanManageSharing
import me.proton.core.drive.drivelink.shared.presentation.entry.ShareUserOptionEntry
import me.proton.core.drive.drivelink.shared.presentation.extension.toViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.usecase.DeleteMember
import me.proton.core.drive.share.user.domain.usecase.GetMemberFlow
import me.proton.core.drive.share.user.domain.usecase.UpdateMemberPermissions
import javax.inject.Inject
import me.proton.core.drive.base.data.extension.log as logResult

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShareMemberOptionsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDriveLink,
    getMemberFlow: GetMemberFlow,
    private val getUserEmailsFlow: GetUserEmailsFlow,
    private val updateMemberPermissions: UpdateMemberPermissions,
    private val deleteMember: DeleteMember,
    private val canManageSharing: CanManageSharing,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val memberId: String = savedStateHandle.require(KEY_MEMBER_ID)

    private val linkId = FileId(
        ShareId(userId, savedStateHandle.require(KEY_SHARE_ID)),
        savedStateHandle.require(KEY_LINK_ID)
    )

    private val driveLink: StateFlow<DriveLink?> = getDriveLink(linkId = linkId).mapSuccessValueOrNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val member = driveLink.filterNotNull().transformLatest { driveLink ->
        emitAll(
            getMemberFlow(
                requireNotNull(driveLink.sharingDetails?.shareId) { "Sharing share id cannot be null" },
                memberId
            )
        )
    }

    val viewState: Flow<ShareUserViewState> = member.map { invitee ->
        invitee.toViewState(appContext)
    }

    fun entries(
        runAction: RunAction,
        navigateToConfirmChangeAccessToViewer: (LinkId, String) -> Unit,
    ): Flow<List<ShareUserOptionEntry>> = combine(
        driveLink.filterNotNull(),
        member,
        canManageSharing(linkId),
        getUserEmailsFlow(userId),
    ) { driveLink, member, canManageSharing, userEmails ->
        options.filter { canManageSharing }.map { option ->
            when (option) {
                is MemberOption.PermissionsViewer -> option.build(
                    isSelected = member.permissions == Permissions.viewer,
                    runAction = runAction,
                ) {
                    if (member.email in userEmails && member.permissions.canWrite) {
                        navigateToConfirmChangeAccessToViewer(linkId, memberId)
                    } else {
                        viewModelScope.launch {
                            updatePermissions(driveLink, Permissions.viewer)
                        }
                    }
                }

                is MemberOption.PermissionsEditor -> option.build(
                    isSelected = member.permissions == Permissions.editor || member.permissions.isAdmin,
                    runAction = runAction,
                ) {
                    viewModelScope.launch {
                        updatePermissions(driveLink, Permissions.editor)
                    }
                }

                is MemberOption.RemoveAccess -> option.build(runAction) {
                    viewModelScope.launch {
                        deleteMember(driveLink)
                    }
                }
            }
        }
    }

    private suspend fun updatePermissions(
        driveLink: DriveLink,
        permissions: Permissions,
    ) {
        val dataResult = updateMemberPermissions(
            shareId = requireNotNull(driveLink.sharingDetails?.shareId),
            memberId = memberId,
            permissions = permissions,
        ).filterSuccessOrError()
            .last()
        dataResult.onFailure { error ->
            error.logResult(VIEW_MODEL, "Failed to update member permissions for $memberId")
            broadcastMessages(
                userId = userId,
                message = error.getDefaultMessage(
                    context = appContext,
                    useExceptionMessage = configurationProvider.useExceptionMessage
                ),
                type = BroadcastMessage.Type.WARNING,
            )
        }
    }

    private suspend fun deleteMember(driveLink: DriveLink) {
        val dataResult = deleteMember(
            shareId = requireNotNull(driveLink.sharingDetails?.shareId),
            memberId = memberId,
        ).filterSuccessOrError().last()
        dataResult.onFailure { error ->
            error.logResult(VIEW_MODEL, "Failed to delete member for $memberId")
            broadcastMessages(
                userId = userId,
                message = error.getDefaultMessage(
                    context = appContext,
                    useExceptionMessage = configurationProvider.useExceptionMessage
                ),
                type = BroadcastMessage.Type.WARNING,
            )
        }
    }

    companion object {
        const val KEY_SHARE_ID = "shareId"
        const val KEY_LINK_ID = "linkId"
        const val KEY_MEMBER_ID = "memberId"

        private val options = setOf(
            MemberOption.PermissionsViewer,
            MemberOption.PermissionsEditor,
            MemberOption.RemoveAccess,
        )
    }
}
