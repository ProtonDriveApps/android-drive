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

package me.proton.core.drive.share.user.data.api.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto

@Serializable
data class ShareInvitationDto(
    @SerialName(Dto.INVITATION_ID)
    val id: String,
    @SerialName(Dto.INVITER_EMAIL)
    val inviterEmail: String,
    @SerialName(Dto.INVITEE_EMAIL)
    val inviteeEmail: String,
    @SerialName(Dto.PERMISSIONS)
    val permissions: Long,
    @SerialName(Dto.KEY_PACKET)
    val keyPacket: String,
    @SerialName(Dto.KEY_PACKET_SIGNATURE)
    val keyPacketSignature: String,
    @SerialName(Dto.CREATE_TIME)
    val createTime: Long,
)
