/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.share.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.CODE
import me.proton.core.drive.base.data.api.Dto.ERROR
import me.proton.core.drive.base.data.api.Dto.ERRORS
import me.proton.core.drive.base.data.api.Dto.SHARE_ID
import me.proton.core.drive.base.data.api.Dto.SHARE_IDS

@Serializable
data class UpdateUnmigratedSharesResponse(
    @SerialName(CODE)
    val code: Long,
    @SerialName(SHARE_IDS)
    val shareIds: List<String>,
    @SerialName(ERRORS)
    val errors: List<UpdateUnmigratedSharesError>,
)

@Serializable
data class UpdateUnmigratedSharesError(
    @SerialName(CODE)
    val code: Long,
    @SerialName(SHARE_ID)
    val shareId: String,
    @SerialName(ERROR)
    val error: String,
)
