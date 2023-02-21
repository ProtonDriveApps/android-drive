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
package me.proton.core.drive.cryptobase.domain.usecase

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.decrypt
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.key.domain.encryptAndSignNestedKey
import me.proton.core.key.domain.entity.key.NestedPrivateKey
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.extension.publicKeyRing
import me.proton.core.key.domain.signData
import me.proton.core.key.domain.useKeys
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EncryptAndSignNestedPrivateKey @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val changeMessage: ChangeMessage,
) {
    suspend operator fun invoke(
        encryptKey: KeyHolder,
        signKey: KeyHolder,
        nestedPrivateKey: NestedPrivateKey,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<NestedPrivateKey> = coRunCatching(coroutineContext) {
        signKey.useKeys(cryptoContext) {
            encryptAndSignNestedKey(
                nestedPrivateKey = nestedPrivateKey,
                encryptKeyRing = encryptKey.publicKeyRing(cryptoContext)
            )
        }
    }

    suspend operator fun invoke(
        decryptKey: KeyHolder,
        encryptKey: KeyHolder,
        signKey: KeyHolder,
        nestedPrivateKey: NestedPrivateKey,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<NestedPrivateKey> = coRunCatching(coroutineContext) {
        signKey.useKeys(cryptoContext) {
            val passphrase = String(requireNotNull(nestedPrivateKey.privateKey.passphrase).decrypt(context.keyStoreCrypto).array)
            nestedPrivateKey.copy(
                privateKey = nestedPrivateKey.privateKey.copy(isActive = false, passphrase = null),
                passphrase = changeMessage(
                    oldMessage = requireNotNull(nestedPrivateKey.passphrase),
                    oldMessageDecryptionKey = decryptKey,
                    newMessage = passphrase,
                    newMessageEncryptionKey = encryptKey,
                    coroutineContext = coroutineContext,
                    signKey = signKey,
                ).getOrThrow(),
                passphraseSignature = signData(passphrase.toByteArray())
            )
        }
    }
}
