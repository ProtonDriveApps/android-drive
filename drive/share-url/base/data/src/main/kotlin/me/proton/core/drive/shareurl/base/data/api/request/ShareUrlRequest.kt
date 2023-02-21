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

package me.proton.core.drive.shareurl.base.data.api.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto

@Serializable
data class ShareUrlRequest(
    /**
     * UNIX timestamp after which this link is no longer accessible.
     * Use this or ExpirationDuration for a relative expiration period. Max 90 days from now.
     */
    @SerialName(Dto.EXPIRATION_TIME)
    val expirationTime: Long? = null,
    /**
     * number of seconds after which this link is no longer accessible. Maximum 90 days.
     */
    @SerialName(Dto.EXPIRATION_DURATION)
    val expirationDuration: Long? = null,
    /**
     * Maximum number of times this link can be accessed. 0 for infinite
     */
    @SerialName(Dto.MAX_ACCESSES)
    val maxAccesses: Long? = null,
    @SerialName(Dto.CREATOR_EMAIL)
    val creatorEmail: String,
    /**
     * Permissions. Bitmap: * - 4: read * - 2: write * Only 4 (read) is allowed currently.
     */
    @SerialName(Dto.PERMISSIONS)
    val permissions: Long,
    /**
     * URL password salt, base64 encoded.
     */
    @SerialName(Dto.URL_PASSWORD_SALT)
    val urlPasswordSalt: String,
    /**
     * Share password salt, base64 encoded.
     */
    @SerialName(Dto.SHARE_PASSWORD_SALT)
    val sharePasswordSalt: String,
    @SerialName(Dto.SRP_VERIFIER)
    val srpVerifier: String,
    @SerialName(Dto.SRP_MODULUS_ID)
    val srpModulusID: String,
    @SerialName(Dto.FLAGS)
    /**
     * 1: custom password set, 2: random password set
     */
    val flags: Long,
    /**
     * PGP encrypted passphrase.
     */
    @SerialName(Dto.SHARE_PASSPHRASE_KEY_PACKET)
    val sharePassphraseKeyPacket: String,
    /**
     * PGP encrypted password. The password is encrypted with the user's address key.
     */
    @SerialName(Dto.PASSWORD)
    val encryptedUrlPassword: String,
    /**
     * PGP encrypted name. The name is encrypted with the user's address key. The name is only for user convenience.
     */
    @SerialName(Dto.NAME)
    val name: String?,
)
