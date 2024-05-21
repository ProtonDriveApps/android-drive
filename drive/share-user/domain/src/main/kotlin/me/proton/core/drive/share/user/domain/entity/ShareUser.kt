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

package me.proton.core.drive.share.user.domain.entity

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS

sealed interface ShareUser {
    val id: String
    val inviter: String
    val email: String
    val createTime: TimestampS
    val permissions: Permissions
    val displayName: String?

    data class Member(
        override val id: String,
        override val inviter: String,
        override val email: String,
        override val createTime: TimestampS,
        override val permissions: Permissions,
        override val displayName: String? = null,
        val keyPacket: String,
        val keyPacketSignature: String,
        val sessionKeySignature: String,
    ) : ShareUser

    data class Invitee(
        override val id: String,
        override val inviter: String,
        override val email: String,
        override val createTime: TimestampS,
        override val permissions: Permissions,
        override val displayName: String? = null,
        val keyPacket: String,
        val keyPacketSignature: String,
    ) : ShareUser

    data class ExternalInvitee(
        override val id: String,
        override val inviter: String,
        override val email: String,
        override val createTime: TimestampS,
        override val permissions: Permissions,
        override val displayName: String? = null,
    ) : ShareUser
}
