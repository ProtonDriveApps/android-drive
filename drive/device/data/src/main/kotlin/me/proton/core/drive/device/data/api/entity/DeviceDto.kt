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

package me.proton.core.drive.device.data.api.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto

@Serializable
data class DeviceDto(
    @SerialName(Dto.DEVICE)
    val device: DeviceDataDto,
    @SerialName(Dto.SHARE)
    val share: ShareDataDto
) {
    @Serializable
    data class DeviceDataDto(
        @SerialName(Dto.DEVICE_ID)
        val deviceId: String,
        @SerialName(Dto.VOLUME_ID)
        val volumeId: String,
        @SerialName(Dto.CREATE_TIME)
        val createTime: Long,
        @SerialName(Dto.MODIFY_TIME)
        val modifyTime: Long? = null,
        @SerialName(Dto.LAST_SYNC_TIME)
        val lastSyncTime: Long? = null,
        @SerialName(Dto.TYPE)
        val type: Long,
        @SerialName(Dto.SYNC_STATE)
        val syncState: Long,
    )

    @Serializable
    data class ShareDataDto(
        @SerialName(Dto.SHARE_ID)
        val shareId: String,
        @SerialName(Dto.LINK_ID)
        val linkId: String,
        @SerialName(Dto.NAME)
        val name: String,
    )
}
