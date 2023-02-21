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

package me.proton.core.drive.link.data.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.HASH
import me.proton.core.drive.base.data.api.Dto.NAME
import me.proton.core.drive.base.data.api.Dto.NAME_SIGNATURE_EMAIL
import me.proton.core.drive.base.data.api.Dto.NODE_PASSPHRASE
import me.proton.core.drive.base.data.api.Dto.ORIGINAL_HASH
import me.proton.core.drive.base.data.api.Dto.PARENT_LINK_ID

@Serializable
data class MoveLinkRequest(
    @SerialName(NAME)
    val name: String,
    @SerialName(HASH)
    val hash: String,
    @SerialName(ORIGINAL_HASH)
    val originalHash: String,
    @SerialName(PARENT_LINK_ID)
    val parentLinkId: String,
    @SerialName(NODE_PASSPHRASE)
    val nodePassphrase: String,
    @SerialName(NAME_SIGNATURE_EMAIL)
    val nameSignatureEmail: String,
)
