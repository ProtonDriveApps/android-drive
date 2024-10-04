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

package me.proton.core.drive.share.user.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.share.crypto.domain.entity.ShareInvitationRequest
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.data.api.ShareInvitationApiDataSource
import me.proton.core.drive.share.user.data.api.entities.ShareExternalInvitationRequestDto
import me.proton.core.drive.share.user.data.api.entities.ShareInvitationRequestDto
import me.proton.core.drive.share.user.data.api.request.CreateShareExternalInvitationRequest
import me.proton.core.drive.share.user.data.api.request.CreateShareInvitationRequest
import me.proton.core.drive.share.user.data.api.request.InvitationEmailDetailsRequestDto
import me.proton.core.drive.share.user.data.api.request.UpdateShareInvitationRequest
import me.proton.core.drive.share.user.data.db.ShareUserDatabase
import me.proton.core.drive.share.user.data.extension.toEntity
import me.proton.core.drive.share.user.data.extension.toShareUserExternalInvitee
import me.proton.core.drive.share.user.data.extension.toShareUserInvitee
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.repository.ShareInvitationRepository
import javax.inject.Inject

class ShareInvitationRepositoryImpl @Inject constructor(
    private val api: ShareInvitationApiDataSource,
    private val db: ShareUserDatabase
) : ShareInvitationRepository {

    override suspend fun hasInvitations(shareId: ShareId): Boolean =
        db.shareInvitationDao.hasInvitations(shareId.userId, shareId.id)

    override suspend fun fetchInvitations(shareId: ShareId): List<ShareUser.Invitee> {
        val invitees = api.getInvitations(
            userId = shareId.userId,
            shareId = shareId.id,
        ).let { response ->
            response.invitations.map { invitation ->
                invitation.toShareUserInvitee()
            }
        }
        db.shareInvitationDao.deleteAll(shareId.userId, shareId.id)
        db.shareInvitationDao.insertOrUpdate(*invitees.map { invitee ->
            invitee.toEntity(shareId)
        }.toTypedArray())
        return invitees
    }

    override fun getInvitationsFlow(
        shareId: ShareId,
        limit: Int,
    ): Flow<List<ShareUser.Invitee>> =
        db.shareInvitationDao.getInvitationsFlow(
            userId = shareId.userId,
            shareId = shareId.id,
            limit = limit,
        ).map { invitations ->
            invitations.map { invitation ->
                invitation.toShareUserInvitee()
            }
        }

    override fun getInvitationFlow(
        shareId: ShareId,
        invitationId: String,
    ): Flow<ShareUser.Invitee> =
        db.shareInvitationDao.getInvitationFlow(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId,
        ).filterNotNull().map { invitation ->
            invitation.toShareUserInvitee()
        }

    override suspend fun createInvitation(
        shareId: ShareId,
        request: ShareInvitationRequest.Internal,
    ) = api.postInvitation(
            userId = shareId.userId,
            shareId = shareId.id,
            request = CreateShareInvitationRequest(
                invitation = ShareInvitationRequestDto(
                    inviterEmail = request.inviterEmail,
                    inviteeEmail = request.inviteeEmail,
                    permissions = request.permissions.value,
                    keyPacket = request.keyPacket,
                    keyPacketSignature = request.keyPacketSignature,
                    externalInvitationId = request.externalInvitationId,
                ),
                emailDetails = InvitationEmailDetailsRequestDto(
                    message = request.message,
                    itemName = request.itemName,
                ),
            )
        ).invitation.toShareUserInvitee().also { shareUserInvitee ->
            db.shareInvitationDao.insertOrUpdate(
                shareUserInvitee.toEntity(shareId)
            )
        }

    override suspend fun updateInvitation(
        shareId: ShareId,
        invitationId: String,
        permissions: Permissions,
    ) {
        api.updateInvitation(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId,
            request = UpdateShareInvitationRequest(
                permissions = permissions.value
            )
        )
        db.shareInvitationDao.updatePermission(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId,
            permissions = permissions.value,
        )
    }

    override suspend fun deleteInvitation(shareId: ShareId, invitationId: String) {
        api.deleteInvitation(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId
        )
        db.shareInvitationDao.deleteInvitation(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId,
        )
    }

    override suspend fun resendInvitation(shareId: ShareId, invitationId: String) {
        api.sendEmail(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId
        )
    }

}
