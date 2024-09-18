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
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.asSuccess
import me.proton.core.drive.base.domain.repository.fetcher
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingDisabled
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.driveSharingExternalInvitationsDisabled
import me.proton.core.drive.feature.flag.domain.extension.off
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.share.crypto.domain.entity.ShareInvitationRequest
import me.proton.core.drive.share.crypto.domain.usecase.CreateShareInvitationRequest
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.entity.ShareUserInvitation
import me.proton.core.drive.share.user.domain.repository.ShareExternalInvitationRepository
import me.proton.core.drive.share.user.domain.repository.ShareInvitationRepository
import javax.inject.Inject

class CreateShareInvitation @Inject constructor(
    private val repository: ShareInvitationRepository,
    private val externalRepository: ShareExternalInvitationRepository,
    private val createShareInvitationRequest: CreateShareInvitationRequest,
    private val getFeatureFlag: GetFeatureFlag,
) {
    operator fun invoke(
        shareId: ShareId,
        invitation: ShareUserInvitation,
    ): Flow<DataResult<ShareUser>> = invoke(
        shareId = shareId,
        email = invitation.email,
        permissions = invitation.permissions
    )

    operator fun invoke(
        shareId: ShareId,
        email: String,
        permissions: Permissions,
        externalInvitationId: String? = null
    ): Flow<DataResult<ShareUser>> = flow {
        emit(DataResult.Processing(ResponseSource.Local))
        createShareInvitationRequest(
            shareId = shareId,
            inviteeEmail = email,
            permissions = permissions,
            externalInvitationId = externalInvitationId,
        ).onFailure { error ->
            emit(DataResult.Error.Local(
                message = "Cannot create invitation request for ${shareId.id}",
                cause = error,
            ))
        }.onSuccess { request ->
            fetcher {
                when(request){
                    is ShareInvitationRequest.Internal -> {
                        check(getFeatureFlag(driveSharingDisabled(shareId.userId)).off) {
                            "Cannot send invitation (feature killed)"
                        }
                        emit(repository.createInvitation(shareId, request).asSuccess)
                    }
                    is ShareInvitationRequest.External -> {
                        check(getFeatureFlag(driveSharingExternalInvitationsDisabled(shareId.userId)).off) {
                            "Cannot send external invitation (feature killed)"
                        }
                        emit(externalRepository.createExternalInvitation(shareId, request).asSuccess)
                    }
                }
            }
        }
    }
}
