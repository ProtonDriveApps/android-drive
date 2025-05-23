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
import me.proton.core.drive.base.data.api.Dto.CODE
import me.proton.core.drive.base.data.api.Dto.LINKS
import me.proton.core.drive.base.data.api.Dto.LINK_ID
import me.proton.core.drive.base.data.api.Dto.MORE
import me.proton.core.drive.base.data.api.Dto.SHARE_ID
import me.proton.core.drive.base.data.api.Dto.SHARE_TARGET_TYPE
import me.proton.core.drive.base.data.api.Dto.VOLUME_ID

@Serializable
data class GetSharedWithMeListingsResponse(
    @SerialName(CODE)
    val code: Long,
    @SerialName(LINKS)
    val links: List<LinkSharedWithMeResponseDto>,
    @SerialName(ANCHOR_ID)
    val anchorId: String? = null,
    @SerialName(MORE)
    val more: Boolean,
)

@Serializable
data class LinkSharedWithMeResponseDto(
    @SerialName(VOLUME_ID)
    val volumeId: String,
    @SerialName(SHARE_ID)
    val shareId: String,
    @SerialName(LINK_ID)
    val linkId: String,
    @SerialName(SHARE_TARGET_TYPE)
    val shareTargetType: Long? = null,
)
