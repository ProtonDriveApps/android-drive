/*
 * Copyright (c) 2021-2023 Proton AG.
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
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.crypto.common.pgp.Unarmored
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.key.domain.encryptAndSignData
import me.proton.core.key.domain.encryptAndSignText
import me.proton.core.key.domain.encryptData
import me.proton.core.key.domain.encryptText
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.useKeysAs
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EncryptText @Inject constructor(
    private val cryptoContext: CryptoContext,
) {
    suspend operator fun invoke(
        encryptKey: KeyHolder,
        text: String,
        signKey: KeyHolder? = null,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<String> = coRunCatching(coroutineContext) {
        encryptKey.useKeysAs(cryptoContext) { key ->
            signKey?.useKeysAs(cryptoContext) { signKey ->
                signKey.encryptAndSignText(text, key.publicKeyRing)
            } ?: key.encryptText(text)
        }
    }

    suspend operator fun invoke(
        sessionKey: SessionKey,
        text: String,
        signKey: KeyHolder? = null,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<Unarmored> = coRunCatching(coroutineContext) {
        signKey?.useKeysAs(cryptoContext) { key ->
            sessionKey.encryptAndSignData(
                context = cryptoContext,
                byteArray = text.toByteArray(),
                unlockedKey = key.privateKeyRing.unlockedPrimaryKey.unlockedKey.value
            )
        } ?: sessionKey.encryptData(cryptoContext, text.toByteArray())
    }
}
