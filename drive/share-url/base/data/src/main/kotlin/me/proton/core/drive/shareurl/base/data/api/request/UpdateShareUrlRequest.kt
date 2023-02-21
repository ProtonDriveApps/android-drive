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
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlCustomPasswordInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlExpirationDurationInfo

@Serializable
data class UpdateShareUrlRequest(
    @SerialName(Dto.FLAGS)
    val flags: Long,
    @SerialName(Dto.SHARE_PASSPHRASE_KEY_PACKET)
    val sharePassphraseKeyPacket: String,
    @SerialName(Dto.PASSWORD)
    val encryptedUrlPassword: String,
    @SerialName(Dto.URL_PASSWORD_SALT)
    val urlPasswordSalt: String,
    @SerialName(Dto.SHARE_PASSWORD_SALT)
    val sharePasswordSalt: String,
    @SerialName(Dto.SRP_VERIFIER)
    val srpVerifier: String,
    @SerialName(Dto.SRP_MODULUS_ID)
    val srpModulusID: String,
    @SerialName(Dto.EXPIRATION_DURATION)
    val expirationDuration: Long?,
) {
    constructor(
        shareUrlCustomPasswordInfo: ShareUrlCustomPasswordInfo,
        shareUrlExpirationDurationInfo: ShareUrlExpirationDurationInfo,
    ) : this(
        flags = shareUrlCustomPasswordInfo.flags.value,
        sharePassphraseKeyPacket = shareUrlCustomPasswordInfo.sharePassphraseKeyPacket,
        encryptedUrlPassword = shareUrlCustomPasswordInfo.encryptedUrlPassword,
        urlPasswordSalt = shareUrlCustomPasswordInfo.urlPasswordSalt,
        sharePasswordSalt = shareUrlCustomPasswordInfo.sharePasswordSalt,
        srpVerifier = shareUrlCustomPasswordInfo.srpVerifier,
        srpModulusID = shareUrlCustomPasswordInfo.srpModulusID,
        expirationDuration = shareUrlExpirationDurationInfo.expirationDuration,
    )
}
