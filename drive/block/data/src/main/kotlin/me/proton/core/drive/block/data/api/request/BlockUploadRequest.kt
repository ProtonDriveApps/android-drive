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
package me.proton.core.drive.block.data.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.ADDRESS_ID
import me.proton.core.drive.base.data.api.Dto.BLOCK_LIST
import me.proton.core.drive.base.data.api.Dto.LINK_ID
import me.proton.core.drive.base.data.api.Dto.REVISION_ID
import me.proton.core.drive.base.data.api.Dto.SHARE_ID
import me.proton.core.drive.base.data.api.Dto.THUMBNAIL_LIST
import me.proton.core.drive.block.data.api.entity.UploadBlockDto
import me.proton.core.drive.block.data.api.entity.UploadThumbnailDto

@Serializable
data class BlockUploadRequest(
    @SerialName(BLOCK_LIST)
    val blockList: List<UploadBlockDto>,
    @SerialName(ADDRESS_ID)
    val addressId: String,
    @SerialName(SHARE_ID)
    val shareId: String,
    @SerialName(LINK_ID)
    val linkId: String,
    @SerialName(REVISION_ID)
    val revisionId: String,
    @SerialName(THUMBNAIL_LIST)
    val thumbnailList: List<UploadThumbnailDto>
)
