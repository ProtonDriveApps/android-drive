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
package me.proton.core.drive.crypto.data.usecase.share

import android.util.Base64
import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.crypto.common.pgp.KeyPacket
import me.proton.core.drive.crypto.domain.entity.ReencryptedSharePassphrase
import me.proton.core.drive.crypto.domain.usecase.share.ReencryptSharePassphraseWithUrlPassword
import me.proton.core.drive.cryptobase.domain.usecase.EncryptSessionKey
import me.proton.core.drive.cryptobase.domain.usecase.GetSessionKeyFromEncryptedMessage
import me.proton.core.drive.cryptobase.domain.usecase.UseSaltedPassword
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import javax.inject.Inject

class ReencryptSharePassphraseWithUrlPasswordImpl @Inject constructor(
    private val getSessionKeyFromEncryptedMessage: GetSessionKeyFromEncryptedMessage,
    private val useSaltedPassword: UseSaltedPassword,
    private val encryptSessionKey: EncryptSessionKey,
) : ReencryptSharePassphraseWithUrlPassword {

    override suspend operator fun invoke(
        decryptKey: KeyHolder,
        urlPassword: ByteArray,
        sharePassphrase: Armored,
    ): Result<ReencryptedSharePassphrase> = runCatching {
        val sessionKey = getSessionKeyFromEncryptedMessage(
            decryptKey = decryptKey,
            message = sharePassphrase,
        ).getOrThrow()
        lateinit var sharePasswordSalt: String
        lateinit var sharePassphraseKeyPacket: KeyPacket
        useSaltedPassword(urlPassword) { saltedPassword, salt ->
            sharePasswordSalt = salt
            sharePassphraseKeyPacket = encryptSessionKey(saltedPassword, sessionKey).getOrThrow()
        }.getOrThrow()
        ReencryptedSharePassphrase(
            sharePasswordSalt = sharePasswordSalt,
            sharePassphraseKeyPacketBase64 = Base64.encodeToString(
                sharePassphraseKeyPacket,
                Base64.NO_WRAP
            ),
        )
    }
}
