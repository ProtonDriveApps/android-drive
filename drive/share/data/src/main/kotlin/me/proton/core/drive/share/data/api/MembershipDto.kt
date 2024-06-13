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

package me.proton.core.drive.share.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.ADDRESS_ID
import me.proton.core.drive.base.data.api.Dto.CREATION_TIME
import me.proton.core.drive.base.data.api.Dto.INVITER
import me.proton.core.drive.base.data.api.Dto.KEY_PACKET
import me.proton.core.drive.base.data.api.Dto.KEY_PACKET_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.MEMBER_ID
import me.proton.core.drive.base.data.api.Dto.PERMISSIONS
import me.proton.core.drive.base.data.api.Dto.SESSION_KEY_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.SHARE_ID

@Serializable
data class MembershipDto(
    @SerialName(MEMBER_ID)
    val memberId: String,
    @SerialName(SHARE_ID)
    val shareId: String,
    @SerialName(INVITER)
    val inviter: String,
    @SerialName(CREATION_TIME)
    val creationTime: Long,
    @SerialName(PERMISSIONS)
    val permissions: Long,
    @SerialName(ADDRESS_ID)
    val addressId: String,
    @SerialName(KEY_PACKET)
    val keyPacket: String,
    @SerialName(KEY_PACKET_SIGNATURE)
    val keyPacketSignature: String?,
    @SerialName(SESSION_KEY_SIGNATURE)
    val sessionKeySignature: String?,
)
