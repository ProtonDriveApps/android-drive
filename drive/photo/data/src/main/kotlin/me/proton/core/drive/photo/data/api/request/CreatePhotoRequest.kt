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

package me.proton.core.drive.photo.data.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.ADDRESS_ID
import me.proton.core.drive.base.data.api.Dto.KEY
import me.proton.core.drive.base.data.api.Dto.LINK
import me.proton.core.drive.base.data.api.Dto.NAME
import me.proton.core.drive.base.data.api.Dto.NODE_HASH_KEY
import me.proton.core.drive.base.data.api.Dto.NODE_KEY
import me.proton.core.drive.base.data.api.Dto.NODE_PASSPHRASE
import me.proton.core.drive.base.data.api.Dto.NODE_PASSPHRASE_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.PASSPHRASE
import me.proton.core.drive.base.data.api.Dto.PASSPHRASE_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.SHARE

@Serializable
data class CreatePhotoRequest(
    @SerialName(SHARE)
    val share: Share,
    @SerialName(LINK)
    val link: Link,
)

@Serializable
data class Share(
    @SerialName(ADDRESS_ID)
    val addressId: String,
    @SerialName(KEY)
    val key: String,
    @SerialName(PASSPHRASE)
    val passphrase: String,
    @SerialName(PASSPHRASE_SIGNATURE)
    val passphraseSignature: String,
)

@Serializable
data class Link(
    @SerialName(NODE_KEY)
    val nodeKey: String,
    @SerialName(NODE_PASSPHRASE)
    val nodePassphrase: String,
    @SerialName(NODE_PASSPHRASE_SIGNATURE)
    val nodePassphraseSignature: String,
    @SerialName(NODE_HASH_KEY)
    val nodeHashKey: String,
    @SerialName(NAME)
    val name: String,
)
