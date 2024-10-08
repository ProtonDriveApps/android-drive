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

package me.proton.core.drive.share.crypto.domain.entity

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.user.domain.entity.AddressId

sealed interface ShareInvitationRequest {

    val inviteeEmail: String
    val permissions: Permissions
    val message: String?
    val itemName: String?
    data class Internal(
        override val inviteeEmail: String,
        override val permissions: Permissions,
        val inviterEmail: String,
        val keyPacket: String,
        val keyPacketSignature: String,
        override val message: String? = null,
        override val itemName: String? = null,
        val externalInvitationId: String? = null,
    ) : ShareInvitationRequest

    data class External(
        override val inviteeEmail: String,
        override val permissions: Permissions,
        val inviterAddressId: AddressId,
        val invitationSignature: String,
        override val message: String? = null,
        override val itemName: String? = null,
    ) : ShareInvitationRequest


}
