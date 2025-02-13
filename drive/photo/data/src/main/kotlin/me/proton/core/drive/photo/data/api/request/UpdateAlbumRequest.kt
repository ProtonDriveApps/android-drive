/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.photo.data.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.COVER_LINK_ID
import me.proton.core.drive.base.data.api.Dto.HASH
import me.proton.core.drive.base.data.api.Dto.LINK
import me.proton.core.drive.base.data.api.Dto.NAME
import me.proton.core.drive.base.data.api.Dto.NAME_SIGNATURE_EMAIL
import me.proton.core.drive.base.data.api.Dto.ORIGINAL_HASH
import me.proton.core.drive.base.data.api.Dto.X_ATTR

@Serializable
data class UpdateAlbumRequest(
    @SerialName(COVER_LINK_ID)
    val coverLinkId: String? = null,
    @SerialName(LINK)
    val link: LinkDto? = null
) {

    @Serializable
    data class LinkDto(
        @SerialName(NAME)
        val name: String? = null,
        @SerialName(HASH)
        val hash: String? = null,
        @SerialName(NAME_SIGNATURE_EMAIL)
        val nameSignatureEmail: String? = null,
        @SerialName(ORIGINAL_HASH)
        val originalHash: String? = null,
        @SerialName(X_ATTR)
        val xAttr: String? = null,
    )
}
