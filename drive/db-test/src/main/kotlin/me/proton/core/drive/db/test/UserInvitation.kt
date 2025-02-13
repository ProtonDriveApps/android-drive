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

package me.proton.core.drive.db.test

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.share.user.data.db.entity.UserInvitationDetailsEntity
import me.proton.core.drive.share.user.data.db.entity.UserInvitationIdEntity

suspend fun ShareContext.userInvitation(id: String) {
    userInvitationId(NullableUserInvitationIdEntity(id))
    userInvitationDetails(NullableUserInvitationDetailsEntity(id))
}

suspend fun ShareContext.userInvitationId(userInvitationIdEntity: UserInvitationIdEntity) {
    db.userInvitationIdDao.insertOrIgnore(userInvitationIdEntity)
}


suspend fun ShareContext.userInvitationDetails(userInvitationDetailsEntity: UserInvitationDetailsEntity) {
    db.userInvitationDetailsDao.insertOrIgnore(userInvitationDetailsEntity)
}

@Suppress("FunctionName")
fun ShareContext.NullableUserInvitationIdEntity(
    id: String,
) = UserInvitationIdEntity(
    id = id,
    userId = user.userId,
    volumeId = volume.id,
    shareId = share.id,
)

@Suppress("FunctionName")
fun ShareContext.NullableUserInvitationDetailsEntity(
    id: String,
) = UserInvitationDetailsEntity(
    id = id,
    userId = user.userId,
    volumeId = volume.id,
    shareId = share.id,
    inviterEmail = "inviterEmail",
    inviteeEmail = "inviteeEmail",
    permissions = Permissions.viewer.value,
    keyPacket = "user-invitation-key-packet",
    keyPacketSignature = "user-invitation-key-packet-signature",
    createTime = 0L,
    passphrase = "passphrase",
    shareKey = "shareKey",
    creatorEmail = "creatorEmail",
    type = 0L,
    linkId = "linkId",
    name = "name",
    mimeType = null,
)
