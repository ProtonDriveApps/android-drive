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

package me.proton.core.drive.share.user.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.ANCHOR_ID
import me.proton.core.drive.base.data.api.Dto.INVITATIONS
import me.proton.core.drive.base.data.api.Dto.MORE
import me.proton.core.drive.share.user.data.api.entities.UserInvitationIdDto

@Serializable
data class GetUserInvitationsResponse (
    @SerialName(INVITATIONS)
    val invitations: List<UserInvitationIdDto>,
    @SerialName(ANCHOR_ID)
    val anchorId: String? = null,
    @SerialName(MORE)
    val hasMore: Boolean = false,
)
