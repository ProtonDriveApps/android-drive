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

import kotlinx.coroutines.withContext
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.extension.getAddressKeys
import me.proton.core.drive.domain.extension.isUnlockable
import me.proton.core.key.domain.decryptAndVerifyNestedKeyOrThrow
import me.proton.core.key.domain.entity.key.NestedPrivateKey
import me.proton.core.key.domain.entity.key.PublicKeyRing
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.extension.allowCompromisedKeys
import me.proton.core.key.domain.extension.publicKeyRing
import me.proton.core.key.domain.useKeysAs
import me.proton.core.user.domain.repository.UserAddressRepository
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DecryptNestedPrivateKey @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val userAddressRepository: UserAddressRepository,
    private val validatePassphraseFormat: ValidatePassphraseFormat,
) {
    suspend operator fun invoke(
        decryptKey: KeyHolder,
        key: NestedPrivateKey,
        verifyKeyRing: PublicKeyRing,
        allowCompromisedVerificationKeys: Boolean = false,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<NestedPrivateKey> = coRunCatching(coroutineContext) {
        if (key.privateKey.isUnlockable) {
            key
        } else {
            decryptKey.useKeysAs(cryptoContext) { keys ->
                keys.decryptAndVerifyNestedKeyOrThrow(
                    nestedPrivateKey = key,
                    verifyKeyRing = if (allowCompromisedVerificationKeys) {
                        verifyKeyRing.allowCompromisedKeys()
                    } else {
                        verifyKeyRing
                    },
                    validTokenPredicate = validatePassphraseFormat::invoke,
                )
            }
        }
    }

    suspend operator fun invoke(
        userId: UserId,
        decryptKey: KeyHolder,
        key: NestedPrivateKey,
        signatureAddress: String,
        allowCompromisedVerificationKeys: Boolean = false,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<NestedPrivateKey> = withContext(coroutineContext) {
        invoke(
            decryptKey = decryptKey,
            key = key,
            verifyKeyRing = userAddressRepository.getAddressKeys(userId, signatureAddress)
                .publicKeyRing(cryptoContext),
            allowCompromisedVerificationKeys = allowCompromisedVerificationKeys,
            coroutineContext = coroutineContext,
        )
    }
}
