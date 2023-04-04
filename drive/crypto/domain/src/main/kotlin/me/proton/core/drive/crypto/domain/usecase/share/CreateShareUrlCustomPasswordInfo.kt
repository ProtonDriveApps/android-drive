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

import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.share.CreateShareUrlInfo.Companion.RANDOM_URL_PASSWORD_SIZE
import me.proton.core.drive.cryptobase.domain.usecase.GenerateSrpForShareUrl
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrl
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlCustomPasswordInfo
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlPasswordFlags
import javax.inject.Inject

class CreateShareUrlCustomPasswordInfo @Inject constructor(
    private val decryptUrlPassword: DecryptUrlPassword,
    private val generateSrpForShareUrl: GenerateSrpForShareUrl,
    private val getAddressKeys: GetAddressKeys,
    private val reencryptSharePassphraseWithUrlPassword: ReencryptSharePassphraseWithUrlPassword,
    private val encryptUrlPassword: EncryptUrlPassword,
    private val configurationProvider: ConfigurationProvider,
){
    suspend operator fun invoke(
        share: Share,
        shareUrl: ShareUrl,
        customPassword: String,
    ): Result<ShareUrlCustomPasswordInfo> = coRunCatching {
        require(customPassword.length <= configurationProvider.maxSharedLinkPasswordLength) {
            "Share Url custom password length is too big (${customPassword.length}/${configurationProvider.maxSharedLinkPasswordLength})"
        }
        with (shareUrl) {
            val userId = share.id.userId
            val addressId = requireNotNull(share.addressId) { "Creating ShareUrl requires Share with valid AddressId" }
            require(shareUrl.flags.isRandom) { "Creating ShareUrl requires random password" }
            val randomPassword = decryptUrlPassword(userId, encryptedUrlPassword, creatorEmail)
                .getOrThrow()
                .take(RANDOM_URL_PASSWORD_SIZE)
            val urlPassword = "$randomPassword$customPassword"
            val srpForShareUrl = generateSrpForShareUrl(userId, urlPassword.toByteArray()).getOrThrow()
            val addressKeys = getAddressKeys(userId, addressId)
            val reencryptedSharePassphrase = reencryptSharePassphraseWithUrlPassword(
                decryptKey = addressKeys.keyHolder,
                urlPassword = urlPassword.toByteArray(),
                sharePassphrase = share.passphrase,
            ).getOrThrow()
            val flags = if (customPassword.isNotEmpty()) {
                flags.add(ShareUrlPasswordFlags.Flag.CUSTOM)
            } else {
                flags.remove(ShareUrlPasswordFlags.Flag.CUSTOM)
            }
            ShareUrlCustomPasswordInfo(
                urlPasswordSalt = srpForShareUrl.urlPasswordSalt,
                sharePasswordSalt = reencryptedSharePassphrase.sharePasswordSalt,
                srpVerifier = srpForShareUrl.verifier,
                srpModulusID = srpForShareUrl.modulusId,
                flags = flags,
                sharePassphraseKeyPacket = reencryptedSharePassphrase.sharePassphraseKeyPacketBase64,
                encryptedUrlPassword = encryptUrlPassword(
                    userId = userId,
                    creatorEmail = creatorEmail,
                    urlPassword = urlPassword,
                ).getOrThrow(),
            )
        }
    }
}
