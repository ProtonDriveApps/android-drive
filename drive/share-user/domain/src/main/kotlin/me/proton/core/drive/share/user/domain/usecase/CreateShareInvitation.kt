/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.share.user.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.repository.fetcher
import me.proton.core.drive.share.crypto.domain.usecase.CreateShareInvitationRequest
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.entity.ShareUserInvitation
import me.proton.core.drive.share.user.domain.repository.ShareInvitationRepository
import javax.inject.Inject

class CreateShareInvitation @Inject constructor(
    private val repository: ShareInvitationRepository,
    private val createShareInvitationRequest: CreateShareInvitationRequest,
) {
    operator fun invoke(
        shareId: ShareId,
        inviterEmail: String,
        invitation: ShareUserInvitation,
    ): Flow<DataResult<ShareUser.Invitee>> = flow {
        emit(DataResult.Processing(ResponseSource.Local))
        createShareInvitationRequest(
            shareId = shareId,
            inviterEmail = inviterEmail,
            inviteeEmail = invitation.email,
            permissions = invitation.permissions
        ).onFailure { error ->
            emit(DataResult.Error.Local(
                message = "Cannot create invitation request for ${shareId.id}",
                cause = error,
            ))
        }.onSuccess { request ->
            fetcher {
                emit(repository.createInvitation(shareId, request).asSuccess)
            }
        }
    }
}
