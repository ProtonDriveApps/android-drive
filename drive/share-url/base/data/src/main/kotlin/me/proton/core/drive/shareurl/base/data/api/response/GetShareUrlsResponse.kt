/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.shareurl.base.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.volume.data.api.entity.ShareUrlDto

@Serializable
data class GetShareUrlsResponse(
    @SerialName(Dto.CODE)
    val code: Int,
    @SerialName(Dto.SHARE_URLS)
    val shareUrlDtos: List<ShareUrlDto>,
    @SerialName(Dto.LINKS)
    val linkDtos: Map<String, LinkDto> = emptyMap(),
)
