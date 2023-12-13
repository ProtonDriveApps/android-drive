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
import me.proton.core.drive.base.data.api.Dto.HASH
import me.proton.core.drive.base.data.api.Dto.SIZE
import me.proton.core.drive.base.data.api.Dto.THUMBNAIL_ID
import me.proton.core.drive.base.data.api.Dto.TYPE

@Serializable
data class LinkThumbnailDto(
    @SerialName(THUMBNAIL_ID)
    val thumbnailId: String?,
    @SerialName(TYPE)
    val type: Long,
    @SerialName(HASH)
    val hash: String,
    @SerialName(SIZE)
    val size: Long,
) {
    companion object {
        const val TYPE_DEFAULT = 1L
        const val TYPE_PHOTO = 2L
    }
}
