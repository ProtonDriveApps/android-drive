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

package me.proton.core.drive.share.user.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.share.crypto.domain.entity.ShareInvitationRequest
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser

interface ShareExternalInvitationRepository {

    fun getExternalInvitationsFlow(
        shareId: ShareId,
        limit: Int
    ): Flow<List<ShareUser.ExternalInvitee>>

    fun getExternalInvitationFlow(
        shareId: ShareId,
        invitationId: String
    ): Flow<ShareUser.ExternalInvitee>

    suspend fun createExternalInvitation(
        shareId: ShareId,
        request: ShareInvitationRequest.External,
    ): ShareUser

    suspend fun updateExternalInvitation(
        shareId: ShareId,
        invitationId: String,
        permissions: Permissions,
    )

    suspend fun deleteExternalInvitation(shareId: ShareId, invitationId: String)
    suspend fun resendExternalInvitation(shareId: ShareId, invitationId: String)
    suspend fun fetchExternalInvitations(shareId: ShareId): List<ShareUser.ExternalInvitee>
    suspend fun hasExternalInvitations(shareId: ShareId): Boolean
}
