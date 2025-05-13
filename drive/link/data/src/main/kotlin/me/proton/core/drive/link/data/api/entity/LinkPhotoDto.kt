/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.link.data.api.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.CAPTURE_TIME
import me.proton.core.drive.base.data.api.Dto.CONTENT_HASH
import me.proton.core.drive.base.data.api.Dto.EXIF
import me.proton.core.drive.base.data.api.Dto.HASH
import me.proton.core.drive.base.data.api.Dto.LINK_ID
import me.proton.core.drive.base.data.api.Dto.MAIN_PHOTO_LINK_ID
import me.proton.core.drive.base.data.api.Dto.RELATED_PHOTOS_LINK_IDS

@Serializable
data class LinkPhotoDto(
    @SerialName(LINK_ID)
    val linkId: String,
    @SerialName(CAPTURE_TIME)
    val captureTime: Long,
    @SerialName(MAIN_PHOTO_LINK_ID)
    val mainPhotoLinkId: String? = null,
    @SerialName(HASH)
    val hash: String,
    @SerialName(CONTENT_HASH)
    val contentHash: String? = null,
    @SerialName(EXIF)
    val exif: String? = null,
    @SerialName(RELATED_PHOTOS_LINK_IDS)
    val relatedPhotosLinkIds: List<String> = emptyList(),
)
