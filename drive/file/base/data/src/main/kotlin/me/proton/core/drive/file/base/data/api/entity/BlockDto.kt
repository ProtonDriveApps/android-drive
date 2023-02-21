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
package me.proton.core.drive.file.base.data.api.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.ENC_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.HASH
import me.proton.core.drive.base.data.api.Dto.INDEX
import me.proton.core.drive.base.data.api.Dto.SIGNATURE_EMAIL
import me.proton.core.drive.base.data.api.Dto.URL

@Serializable
data class BlockDto(
    @SerialName(INDEX)
    val index: Long,
    @SerialName(URL)
    val url: String,
    @SerialName(ENC_SIGNATURE)
    val encryptedSignature: String?,
    @SerialName(SIGNATURE_EMAIL)
    val signatureEmail: String?,
    @SerialName(HASH)
    val hashSha256: String?,
)
