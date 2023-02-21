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
package me.proton.core.drive.crypto.domain.usecase.share

import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.usecase.GetUserEmail
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.GeneratePassphrase
import me.proton.core.drive.cryptobase.domain.usecase.GenerateSrpForShareUrl
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlPasswordFlags
import javax.inject.Inject

class CreateShareUrlInfo @Inject constructor(
    private val generatePassphrase: GeneratePassphrase,
    private val getAddressKeys: GetAddressKeys,
    private val generateSrpForShareUrl: GenerateSrpForShareUrl,
    private val reencryptSharePassphraseWithUrlPassword: ReencryptSharePassphraseWithUrlPassword,
    private val getUserEmail: GetUserEmail,
    private val encryptUrlPassword: EncryptUrlPassword,
) {
    suspend operator fun invoke(share: Share): Result<ShareUrlInfo> = coRunCatching {
        val userId = share.id.userId
        val addressId = requireNotNull(share.addressId) // TODO: this can be null if share was not get by bootstrap, try fallback to creator then
        val randomUrlPassword = generatePassphrase(urlPassphraseSize, true)
        require(randomUrlPassword.length == RANDOM_URL_PASSWORD_SIZE) {
            "Random URL password size (${randomUrlPassword.length}) does not match requirement (${RANDOM_URL_PASSWORD_SIZE})"
        }
        val addressKeys = getAddressKeys(userId, addressId)
        val srpForShareUrl = generateSrpForShareUrl(randomUrlPassword.toByteArray()).getOrThrow()
        val reencryptedSharePassphrase = reencryptSharePassphraseWithUrlPassword(
            decryptKey = addressKeys.keyHolder,
            urlPassword = randomUrlPassword.toByteArray(),
            sharePassphrase = share.passphrase,
        ).getOrThrow()
        val creatorEmail = getUserEmail(userId, addressId)
        ShareUrlInfo(
            expirationDuration = null,
            maxAccesses = MAX_ACCESSES,
            creatorEmail = creatorEmail,
            permissions = Permissions().add(Permissions.Permission.READ),
            urlPasswordSalt = srpForShareUrl.urlPasswordSalt,
            sharePasswordSalt = reencryptedSharePassphrase.sharePasswordSalt,
            srpVerifier = srpForShareUrl.verifier,
            srpModulusID = srpForShareUrl.modulusId,
            flags = ShareUrlPasswordFlags().add(ShareUrlPasswordFlags.Flag.RANDOM),
            sharePassphraseKeyPacket = reencryptedSharePassphrase.sharePassphraseKeyPacketBase64,
            encryptedUrlPassword = encryptUrlPassword(
                userId = userId,
                creatorEmail = creatorEmail,
                urlPassword = randomUrlPassword,
            ).getOrThrow(),
            name = null,
        )
    }

    companion object {
        private val urlPassphraseSize = 9.bytes // must be a multiple of 3 to avoid Base64 padding
        val RANDOM_URL_PASSWORD_SIZE = (urlPassphraseSize.value.toInt() / 3) * 4 // Base64 encodes 3 bytes into 4 ASCII characters
        const val MAX_ACCESSES = 0L
    }
}
