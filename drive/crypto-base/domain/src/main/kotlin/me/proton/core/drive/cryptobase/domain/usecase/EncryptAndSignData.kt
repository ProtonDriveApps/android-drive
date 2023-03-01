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
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.key.domain.encryptAndSignData
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.extension.publicKeyRing
import me.proton.core.key.domain.useKeysAs
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EncryptAndSignData @Inject constructor(
    private val cryptoContext: CryptoContext,
){
    suspend operator fun invoke(
        encryptKey: SessionKey,
        verifyKey: KeyHolder,
        input: ByteArray,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ) = coRunCatching(coroutineContext) {
        verifyKey.useKeysAs(cryptoContext) { key ->
            key.encryptAndSignData(input, encryptKey)
        }
    }

    suspend operator fun invoke(
        encryptKey: KeyHolder,
        verifyKey: KeyHolder,
        input: ByteArray,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ) = coRunCatching(coroutineContext) {
        verifyKey.useKeysAs(cryptoContext) { key ->
            key.encryptAndSignData(input, encryptKey.publicKeyRing(cryptoContext))
        }
    }
}