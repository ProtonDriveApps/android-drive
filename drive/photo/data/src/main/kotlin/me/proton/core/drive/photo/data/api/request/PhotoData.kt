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
import me.proton.core.drive.base.data.api.Dto.CONTENT_HASH
import me.proton.core.drive.base.data.api.Dto.HASH
import me.proton.core.drive.base.data.api.Dto.NAME
import me.proton.core.drive.base.data.api.Dto.NAME_SIGNATURE_EMAIL
import me.proton.core.drive.base.data.api.Dto.NODE_PASSPHRASE
import me.proton.core.drive.base.data.api.Dto.NODE_PASSPHRASE_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.RELATED_PHOTOS
import me.proton.core.drive.base.data.api.Dto.SIGNATURE_EMAIL

@Serializable
data class PhotoData(
    @SerialName(HASH)
    val hash: String,
    @SerialName(NAME)
    val name: String,
    @SerialName(NAME_SIGNATURE_EMAIL)
    val nameSignatureEmail: String,
    @SerialName(NODE_PASSPHRASE)
    val nodePassphrase: String,
    @SerialName(CONTENT_HASH)
    val contentHash: String,
    @SerialName(RELATED_PHOTOS)
    val relatedPhotos: List<RelatedPhoto>,
    @SerialName(NODE_PASSPHRASE_SIGNATURE)
    val nodePassphraseSignature: String? = null,
    @SerialName(SIGNATURE_EMAIL)
    val signatureEmail: String? = null,
)
