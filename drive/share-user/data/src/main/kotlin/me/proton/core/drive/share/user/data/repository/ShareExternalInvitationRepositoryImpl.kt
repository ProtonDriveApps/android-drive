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
import me.proton.core.drive.share.user.data.api.ShareExternalInvitationApiDataSource
import me.proton.core.drive.share.user.data.api.entities.ShareExternalInvitationRequestDto
import me.proton.core.drive.share.user.data.api.request.CreateShareExternalInvitationRequest
import me.proton.core.drive.share.user.data.api.request.InvitationEmailDetailsRequestDto
import me.proton.core.drive.share.user.data.api.request.UpdateShareInvitationRequest
import me.proton.core.drive.share.user.data.db.ShareUserDatabase
import me.proton.core.drive.share.user.data.extension.toEntity
import me.proton.core.drive.share.user.data.extension.toShareUserExternalInvitee
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.repository.ShareExternalInvitationRepository
import javax.inject.Inject

class ShareExternalInvitationRepositoryImpl @Inject constructor(
    private val api: ShareExternalInvitationApiDataSource,
    private val db: ShareUserDatabase
) : ShareExternalInvitationRepository {

    override suspend fun hasExternalInvitations(shareId: ShareId): Boolean =
        db.shareExternalInvitationDao.hasInvitations(shareId.userId, shareId.id)

    override suspend fun fetchExternalInvitations(shareId: ShareId): List<ShareUser.ExternalInvitee> {
        val invitees = api.getExternalInvitations(
            userId = shareId.userId,
            shareId = shareId.id,
        ).let { response ->
            response.invitations.map { invitation ->
                invitation.toShareUserExternalInvitee()
            }
        }
        db.inTransaction {
            db.shareExternalInvitationDao.deleteAll(shareId.userId, shareId.id)
            db.shareExternalInvitationDao.insertOrUpdate(*invitees.map { invitee ->
                invitee.toEntity(shareId)
            }.toTypedArray())
        }
        return invitees
    }

    override fun getExternalInvitationsFlow(
        shareId: ShareId,
        limit: Int,
    ): Flow<List<ShareUser.ExternalInvitee>> =
        db.shareExternalInvitationDao.getInvitationsFlow(
            userId = shareId.userId,
            shareId = shareId.id,
            limit = limit,
        ).map { invitations ->
            invitations.map { invitation ->
                invitation.toShareUserExternalInvitee()
            }
        }

    override fun getExternalInvitationFlow(
        shareId: ShareId,
        invitationId: String,
    ): Flow<ShareUser.ExternalInvitee> =
        db.shareExternalInvitationDao.getInvitationFlow(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId,
        ).filterNotNull().map { invitation ->
            invitation.toShareUserExternalInvitee()
        }

    override suspend fun createExternalInvitation(
        shareId: ShareId,
        request: ShareInvitationRequest.External,
    ) = api.postExternalInvitation(
        userId = shareId.userId,
        shareId = shareId.id,
        request = CreateShareExternalInvitationRequest(
            invitation = ShareExternalInvitationRequestDto(
                inviterAddressId = request.inviterAddressId.id,
                inviteeEmail = request.inviteeEmail,
                permissions = request.permissions.value,
                externalInvitationSignature = request.invitationSignature,
            ),
            emailDetails = InvitationEmailDetailsRequestDto(
                message = request.message,
                itemName = request.itemName,
            ),
        )
    ).externalInvitation.toShareUserExternalInvitee().also { externalInvitee ->
        db.shareExternalInvitationDao.insertOrUpdate(
            externalInvitee.toEntity(shareId)
        )
    }

    override suspend fun updateExternalInvitation(
        shareId: ShareId,
        invitationId: String,
        permissions: Permissions,
    ) {
        api.updateExternalInvitation(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId,
            request = UpdateShareInvitationRequest(
                permissions = permissions.value
            )
        )
        db.shareExternalInvitationDao.updatePermission(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId,
            permissions = permissions.value,
        )
    }

    override suspend fun deleteExternalInvitation(shareId: ShareId, invitationId: String) {
        api.deleteExternalInvitation(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId
        )
        db.shareExternalInvitationDao.deleteInvitation(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId,
        )
    }

    override suspend fun resendExternalInvitation(shareId: ShareId, invitationId: String) {
        api.sendExternalEmail(
            userId = shareId.userId,
            shareId = shareId.id,
            invitationId = invitationId
        )
    }

}
