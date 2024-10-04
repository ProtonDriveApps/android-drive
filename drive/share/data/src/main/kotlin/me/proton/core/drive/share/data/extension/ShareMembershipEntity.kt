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

package me.proton.core.drive.share.data.extension

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.share.data.db.ShareMembershipEntity
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.entity.ShareMembership
import me.proton.core.user.domain.entity.AddressId

fun ShareMembershipEntity.toShareMembership() = ShareMembership(
    id = id,
    shareId = ShareId(userId, shareId),
    inviterEmail = inviterEmail,
    inviteeEmail = inviteeEmail,
    addressId = AddressId(addressId),
    permissions = Permissions(permissions),
    keyPacket = keyPacket,
    keyPacketSignature = keyPacketSignature,
    sessionKeySignature = sessionKeySignature,
    createTime = createTime,
)
