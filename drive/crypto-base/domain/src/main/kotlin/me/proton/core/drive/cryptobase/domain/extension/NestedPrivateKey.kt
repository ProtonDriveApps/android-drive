/*
 * Copyright (c) 2021-2024 Proton AG.
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
package me.proton.core.drive.cryptobase.domain.extension

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.decrypt
import me.proton.core.crypto.common.pgp.PGPHeader
import me.proton.core.key.domain.encryptData
import me.proton.core.key.domain.encryptSessionKey
import me.proton.core.key.domain.entity.key.NestedPrivateKey
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.extension.publicKeyRing
import me.proton.core.key.domain.signData
import me.proton.core.key.domain.useKeys

internal fun NestedPrivateKey.encryptAndSignPassphrase(
    encryptKey: KeyHolder,
    signKey: KeyHolder,
    cryptoContext: CryptoContext,
): NestedPrivateKey {
    val passphrase = requireNotNull(privateKey.passphrase) { "Cannot encrypt without passphrase." }
    return passphrase.decrypt(cryptoContext.keyStoreCrypto).use { decryptedPassphrase ->
        copy(
            passphrase = encryptKey.publicKeyRing(cryptoContext).encryptData(cryptoContext, decryptedPassphrase.array),
            passphraseSignature = signKey.useKeys(cryptoContext) { signData(decryptedPassphrase.array) }
        )
    }
}

internal fun NestedPrivateKey.encryptAndSignPassphrase(
    encryptKeys: List<KeyHolder>,
    signKey: KeyHolder,
    cryptoContext: CryptoContext,
): NestedPrivateKey {
    val passphrase = requireNotNull(privateKey.passphrase) { "Cannot encrypt without passphrase." }
    return passphrase.decrypt(cryptoContext.keyStoreCrypto).use { decryptedPassphrase ->
        cryptoContext.pgpCrypto.generateNewSessionKey().use { sessionKey ->
            val dataPacket = cryptoContext.pgpCrypto.encryptData(decryptedPassphrase.array, sessionKey)
            val keyPackets = encryptKeys.map { keyHolder ->
                keyHolder.publicKeyRing(cryptoContext).encryptSessionKey(cryptoContext, sessionKey)
            }
            copy(
                passphrase = cryptoContext.pgpCrypto.getArmored(
                    data = keyPackets.fold(byteArrayOf()) { sum, keyPacket ->
                        sum + keyPacket
                    } + dataPacket,
                    header = PGPHeader.Message,
                ),
                passphraseSignature = signKey.useKeys(cryptoContext) { signData(decryptedPassphrase.array) }
            )
        }
    }
}
