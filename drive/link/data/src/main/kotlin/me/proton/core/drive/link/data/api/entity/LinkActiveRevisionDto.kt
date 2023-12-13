/*
 * Copyright (c) 2021-2023 Proton AG.
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
import me.proton.core.drive.base.data.api.Dto.CREATE_TIME
import me.proton.core.drive.base.data.api.Dto.ID
import me.proton.core.drive.base.data.api.Dto.MANIFEST_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.PHOTO
import me.proton.core.drive.base.data.api.Dto.SIGNATURE_ADDRESS
import me.proton.core.drive.base.data.api.Dto.SIZE
import me.proton.core.drive.base.data.api.Dto.STATE
import me.proton.core.drive.base.data.api.Dto.THUMBNAIL
import me.proton.core.drive.base.data.api.Dto.THUMBNAILS

@Serializable
data class LinkActiveRevisionDto(
    @SerialName(ID)
    val id: String,
    @SerialName(CREATE_TIME)
    val creationTime: Long,
    @SerialName(SIZE)
    val size: Long,
    @SerialName(MANIFEST_SIGNATURE)
    val manifestSignature: String,
    @SerialName(SIGNATURE_ADDRESS)
    val signatureAddress: String?,
    @SerialName(STATE)
    val state: Long,
    @SerialName(THUMBNAIL)
    val thumbnail: Long,
    @SerialName(PHOTO)
    val photo: LinkPhotoDto? = null,
    @SerialName(THUMBNAILS)
    val thumbnails: List<LinkThumbnailDto>,
)
