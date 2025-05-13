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

import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.user.data.db.entity.UserInvitationIdAndDetailsEntity
import me.proton.core.drive.share.user.domain.entity.UserInvitation
import me.proton.core.drive.share.user.domain.entity.UserInvitationDetails
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun UserInvitationIdAndDetailsEntity.toUserInvitationId() = UserInvitation(
    id = UserInvitationId(VolumeId(volumeId), ShareId(userId, shareId), id, shareTargetType?.toShareTargetType()),
    details = details?.run {
        UserInvitationDetails(
            id = UserInvitationId(VolumeId(volumeId), ShareId(userId, shareId), id, shareTargetType?.toShareTargetType()),
            inviterEmail = inviterEmail,
            inviteeEmail = inviteeEmail,
            permissions = Permissions(permissions),
            keyPacket = keyPacket,
            keyPacketSignature = keyPacketSignature,
            createTime = TimestampS(createTime),
            passphrase = passphrase,
            shareKey = shareKey,
            creatorEmail = creatorEmail,
            type = type,
            linkId = linkId,
            cryptoName = CryptoProperty.Encrypted(name),
            mimeType = mimeType
        )
    }
)
