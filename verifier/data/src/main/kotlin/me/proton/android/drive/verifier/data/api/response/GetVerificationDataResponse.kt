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

package me.proton.android.drive.verifier.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.CODE
import me.proton.core.drive.base.data.api.Dto.CONTENT_KEY_PACKET
import me.proton.core.drive.base.data.api.Dto.REVISION
import me.proton.core.drive.base.data.api.Dto.SIGNATURE_ADDRESS
import me.proton.core.drive.base.data.api.Dto.STATE
import me.proton.core.drive.base.data.api.Dto.VERIFICATION_CODE

@Serializable
data class GetVerificationDataResponse(
    @SerialName(CODE)
    val code: Long,
    @SerialName(VERIFICATION_CODE)
    val verificationCode: String,
    @SerialName(CONTENT_KEY_PACKET)
    val contentKeyPacket: String,
)
