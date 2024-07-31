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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import me.proton.android.drive.ui.options.InvitationOption
import me.proton.core.domain.arch.onSuccess
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.shared.domain.extension.sharingDetails
import me.proton.core.drive.drivelink.shared.presentation.extension.toViewState
import me.proton.core.drive.drivelink.shared.presentation.viewstate.ShareUserViewState
import me.proton.core.drive.i18n.R
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.drive.share.user.domain.usecase.DeleteExternalInvitation
import me.proton.core.drive.share.user.domain.usecase.GetExternalInvitationFlow
import me.proton.core.drive.share.user.domain.usecase.ResendExternalInvitation
import me.proton.core.drive.share.user.domain.usecase.UpdateExternalInvitationPermissions
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShareExternalInvitationOptionsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getDriveLink: GetDriveLink,
    getExternalInvitationFlow: GetExternalInvitationFlow,
    private val updateInvitationPermissions: UpdateExternalInvitationPermissions,
    private val resendInvitation: ResendExternalInvitation,
    private val deleteInvitation: DeleteExternalInvitation,
    configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
) : ShareInvitationOptionsViewModel(
    appContext = appContext,
    savedStateHandle = savedStateHandle,
    getDriveLink = getDriveLink,
    configurationProvider = configurationProvider,
    broadcastMessages = broadcastMessages
) {

    override val invitation = driveLink.filterNotNull().transformLatest { driveLink ->
        emitAll(
            getExternalInvitationFlow(
                requireNotNull(driveLink.sharingDetails?.shareId) { "Sharing share id cannot be null" },
                invitationId
            )
        )
    }

    override val options: Set<InvitationOption> = setOf(
        InvitationOption.PermissionsViewer,
        InvitationOption.PermissionsEditor,
        InvitationOption.ResendInvitation,
        InvitationOption.RemoveAccess,
    )

    val viewState: Flow<ShareUserViewState> = invitation.map { invitee ->
        invitee.toViewState(appContext)
    }

    override suspend fun updatePermissions(
        driveLink: DriveLink,
        permissions: Permissions,
    ) {
        updateInvitationPermissions(
            shareId = requireNotNull(driveLink.sharingDetails?.shareId),
            invitationId = invitationId,
            permissions = permissions,
        ).filterSuccessOrError().last().onFailure { error ->
            logAndBroadcastMessage(error)
        }
    }

    override suspend fun deleteInvitation(driveLink: DriveLink) {
        deleteInvitation(
            shareId = requireNotNull(driveLink.sharingDetails?.shareId),
            invitationId = invitationId,
        ).filterSuccessOrError().last().onFailure { error ->
            logAndBroadcastMessage(error)
        }
    }

    override suspend fun resendInvitation(driveLink: DriveLink) {
        resendInvitation(
            shareId = requireNotNull(driveLink.sharingDetails?.shareId),
            invitationId = invitationId,
        ).filterSuccessOrError().last().onFailure { error ->
            logAndBroadcastMessage(error, BroadcastMessage.Type.ERROR)
        }.onSuccess {
            broadcastMessages(
                userId = userId,
                message = appContext.getString(R.string.share_via_invitations_resend_invite_success),
                type = BroadcastMessage.Type.INFO,
            )
        }
    }

    override suspend fun copyInvitationUrl(
        driveLink: DriveLink
    ) { /* do nothing */}

}
