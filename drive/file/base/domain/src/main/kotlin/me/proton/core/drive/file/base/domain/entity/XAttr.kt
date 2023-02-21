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
package me.proton.core.drive.file.base.domain.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class XAttr(
    @SerialName("Common")
    val common: Common,
    @SerialName("Media")
    val media: Media? = null,
) {
    @Serializable
    data class Common(
        @SerialName("ModificationTime")
        val modificationTime: String,
        @SerialName("Size")
        val size: Long? = null,
        @SerialName("BlockSizes")
        val blockSizes: List<Long>? = null,
    )

    @Serializable
    data class Media(
        @SerialName("Width")
        val width: Long,
        @SerialName("Height")
        val height: Long,
    )
}
