/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.shareurl.base.data.api.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.CREATE_TIME
import me.proton.core.drive.base.data.api.Dto.CREATOR_EMAIL
import me.proton.core.drive.base.data.api.Dto.EXPIRATION_TIME
import me.proton.core.drive.base.data.api.Dto.FLAGS
import me.proton.core.drive.base.data.api.Dto.LAST_ACCESS_TIME
import me.proton.core.drive.base.data.api.Dto.MAX_ACCESSES
import me.proton.core.drive.base.data.api.Dto.NAME
import me.proton.core.drive.base.data.api.Dto.NUMBER_OF_ACCESSES
import me.proton.core.drive.base.data.api.Dto.PASSWORD
import me.proton.core.drive.base.data.api.Dto.PERMISSIONS
import me.proton.core.drive.base.data.api.Dto.PUBLIC_URL
import me.proton.core.drive.base.data.api.Dto.SHARE_ID
import me.proton.core.drive.base.data.api.Dto.SHARE_PASSPHRASE_KEY_PACKET
import me.proton.core.drive.base.data.api.Dto.SHARE_PASSWORD_SALT
import me.proton.core.drive.base.data.api.Dto.SHARE_URL_ID
import me.proton.core.drive.base.data.api.Dto.SRP_MODULUS_ID
import me.proton.core.drive.base.data.api.Dto.SRP_VERIFIER
import me.proton.core.drive.base.data.api.Dto.TOKEN
import me.proton.core.drive.base.data.api.Dto.URL_PASSWORD_SALT

@Serializable
data class ShareUrlDto(
    @SerialName(SHARE_URL_ID)
    val shareUrlId: String,
    @SerialName(SHARE_ID)
    val shareId: String,
    @SerialName(TOKEN)
    val token: String,
    @SerialName(NAME)
    val name: String?,
    @SerialName(CREATE_TIME)
    val createTime: Long,
    @SerialName(EXPIRATION_TIME)
    val expirationTime: Long? = null,
    @SerialName(LAST_ACCESS_TIME)
    val lastAccessTime: Long? = null,
    @SerialName(MAX_ACCESSES)
    val maxAccesses: Long? = null,
    @SerialName(NUMBER_OF_ACCESSES)
    val numberOfAccesses: Long? = null,
    @SerialName(CREATOR_EMAIL)
    val creatorEmail: String,
    @SerialName(PERMISSIONS)
    val permissions: Long,
    @SerialName(FLAGS)
    val flags: Long,
    @SerialName(URL_PASSWORD_SALT)
    val urlPasswordSalt: String,
    @SerialName(SHARE_PASSWORD_SALT)
    val sharePasswordSalt: String,
    @SerialName(SRP_VERIFIER)
    val srpVerifier: String,
    @SerialName(SRP_MODULUS_ID)
    val srpModulusId: String,
    @SerialName(PASSWORD)
    val encryptedUrlPassword: String,
    @SerialName(SHARE_PASSPHRASE_KEY_PACKET)
    val sharePassphraseKeyPacket: String,
    @SerialName(PUBLIC_URL)
    val publicUrl: String,
)
