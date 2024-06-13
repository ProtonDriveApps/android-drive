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
import me.proton.core.drive.share.user.data.db.entity.ShareMemberEntity

suspend fun ShareContext.member(
    email: String,
    permissions: Permissions = Permissions()
) {
    member(NullableShareMemberEntity(email, permissions))
}

suspend fun ShareContext.member(shareMemberEntity: ShareMemberEntity) {
    db.shareMemberDao.insertOrIgnore(shareMemberEntity)
}

@Suppress("FunctionName")
fun ShareContext.NullableShareMemberEntity(
    email: String,
    permissions: Permissions,
    id: String = "member-id-$email"
) = ShareMemberEntity(
    id = id,
    userId = user.userId,
    shareId = share.id,
    inviterEmail = "inviter@proton.me",
    inviteeEmail = email,
    permissions = permissions.value,
    keyPacket = "member-key-packet",
    keyPacketSignature = "member-key-packet-signature",
    sessionKeySignature = "member-session-key-signature",
    createTime = 0,
)
