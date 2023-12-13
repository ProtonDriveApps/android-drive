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

package me.proton.core.drive.link.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.AVAILABLE_HASHES
import me.proton.core.drive.base.data.api.Dto.CLIENT_UID
import me.proton.core.drive.base.data.api.Dto.CODE
import me.proton.core.drive.base.data.api.Dto.HASH
import me.proton.core.drive.base.data.api.Dto.LINK_ID
import me.proton.core.drive.base.data.api.Dto.PENDING_HASHES
import me.proton.core.drive.base.data.api.Dto.REVISION_ID

@Serializable
data class CheckAvailableHashesResponse(
    @SerialName(CODE)
    val code: Long,
    @SerialName(AVAILABLE_HASHES)
    val availableHashes: List<String>,
    @SerialName(PENDING_HASHES)
    val pendingHashDtos: List<PendingHashDto>,
)

@Serializable
data class PendingHashDto(
    @SerialName(CLIENT_UID)
    val clientUid: String?,
    @SerialName(HASH)
    val hash: String,
    @SerialName(REVISION_ID)
    val revisionId: String,
    @SerialName(LINK_ID)
    val linkId: String,
)
