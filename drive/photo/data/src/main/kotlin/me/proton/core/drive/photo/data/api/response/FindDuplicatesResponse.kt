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

package me.proton.core.drive.photo.data.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.CLIENT_UID
import me.proton.core.drive.base.data.api.Dto.CODE
import me.proton.core.drive.base.data.api.Dto.CONTENT_HASH
import me.proton.core.drive.base.data.api.Dto.DUPLICATE_HASHES
import me.proton.core.drive.base.data.api.Dto.HASH
import me.proton.core.drive.base.data.api.Dto.LINK_ID
import me.proton.core.drive.base.data.api.Dto.LINK_STATE
import me.proton.core.drive.base.data.api.Dto.REVISION_ID

@Serializable
data class FindDuplicatesResponse(
    @SerialName(CODE)
    val code: Long,
    @SerialName(DUPLICATE_HASHES)
    val duplicates: List<DuplicateDto>,
)

@Serializable
data class DuplicateDto(
    @SerialName(HASH)
    val hash: String,
    @SerialName(CONTENT_HASH)
    val contentHash: String?,
    @SerialName(LINK_STATE)
    val state: Long?,
    @SerialName(CLIENT_UID)
    val clientUid: String?,
    @SerialName(LINK_ID)
    val linkId: String?,
    @SerialName(REVISION_ID)
    val revisionId: String?,
)
