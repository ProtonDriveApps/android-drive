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

package me.proton.core.drive.share.user.data.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.share.user.data.api.response.GetUserInvitationResponse
import me.proton.core.drive.share.user.data.db.entity.UserInvitationDetailsEntity

fun GetUserInvitationResponse.toEntity(userId: UserId) = UserInvitationDetailsEntity(
    userId = userId,
    volumeId = share.volumeId,
    shareId = share.shareId,
    id = invitation.invitationId,
    inviteeEmail = invitation.inviteeEmail,
    inviterEmail = invitation.inviterEmail,
    permissions = invitation.permissions,
    keyPacket = invitation.keyPacket,
    keyPacketSignature = invitation.keyPacketSignature,
    createTime = invitation.createTime,
    passphrase = share.passphrase,
    shareKey = share.shareKey,
    creatorEmail = share.creatorEmail,
    type = link.type,
    linkId = link.linkId,
    name = link.name,
    mimeType = link.mimeType,
)
