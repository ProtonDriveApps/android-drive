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
package me.proton.core.drive.volume.data.api.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.CREATE_TIME
import me.proton.core.drive.base.data.api.Dto.SHARE
import me.proton.core.drive.base.data.api.Dto.STATE
import me.proton.core.drive.base.data.api.Dto.TYPE
import me.proton.core.drive.base.data.api.Dto.USED_SPACE
import me.proton.core.drive.base.data.api.Dto.VOLUME_ID

@Serializable
data class VolumeDto(
    @SerialName(VOLUME_ID)
    val id: String,
    @SerialName(CREATE_TIME)
    val createTime: Long?,
    @SerialName(USED_SPACE)
    val usedSpace: Long,
    @SerialName(STATE)
    val state: Long,
    @SerialName(SHARE)
    val share: VolumeShare,
    @SerialName(TYPE)
    val type: Long,
) {
    companion object {
        const val TYPE_REGULAR = 1L
        const val TYPE_PHOTO = 2L
    }
}
