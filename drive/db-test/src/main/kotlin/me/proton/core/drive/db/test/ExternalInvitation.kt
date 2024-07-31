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

import me.proton.core.drive.share.user.data.db.entity.ShareExternalInvitationEntity

suspend fun ShareContext.externalInvitation(email: String) {
    externalInvitation(NullableShareExternalInvitationEntity(email))
}

suspend fun ShareContext.externalInvitation(shareExternalInvitationEntity: ShareExternalInvitationEntity) {
    db.shareExternalInvitationDao.insertOrIgnore(shareExternalInvitationEntity)
}

@Suppress("FunctionName")
fun ShareContext.NullableShareExternalInvitationEntity(
    email: String,
    id: String = "invitation-id-$email"
) = ShareExternalInvitationEntity(
    id = id,
    userId = user.userId,
    shareId = share.id,
    inviterEmail = "inviter@proton.me",
    inviteeEmail = email,
    permissions = 0,
    signature = "invitation-signature",
    state = 1,
    createTime = 0,
)
