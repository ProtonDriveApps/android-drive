/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.photo.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.CODE
import me.proton.core.drive.base.data.api.Dto.DEVICE
import me.proton.core.drive.base.data.api.Dto.DEVICE_ID
import me.proton.core.drive.base.data.api.Dto.LINK_ID
import me.proton.core.drive.base.data.api.Dto.SHARE
import me.proton.core.drive.base.data.api.Dto.SHARE_ID

@Serializable
data class CreatePhotoResponse(
    @SerialName(CODE)
    val code: Long,
    @SerialName(SHARE)
    val share: Share,
)

@Serializable
data class Share(
    @SerialName(SHARE_ID)
    val shareId: String,
    @SerialName(LINK_ID)
    val linkId: String,
)
