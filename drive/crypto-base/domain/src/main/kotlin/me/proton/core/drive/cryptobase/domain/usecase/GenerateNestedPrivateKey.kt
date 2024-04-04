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
package me.proton.core.drive.cryptobase.domain.usecase

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.extension.encryptAndSignPassphrase
import me.proton.core.drive.cryptobase.domain.extension.getAddressKeys
import me.proton.core.key.domain.entity.key.NestedPrivateKey
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.user.domain.repository.UserAddressRepository
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class GenerateNestedPrivateKey @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val userAddressRepository: UserAddressRepository,
    private val generatePassphrase: GeneratePassphrase,
) {
    @Suppress("UNUSED_PARAMETER")
    suspend operator fun invoke(
        userId: UserId,
        encryptKey: KeyHolder,
        signKey: KeyHolder,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<NestedPrivateKey> = coRunCatching(coroutineContext) {
        NestedPrivateKey
            .generateNestedPrivateKey(cryptoContext, DEFAULT_USERNAME, DEFAULT_DOMAIN, generatePassphrase::invoke)
            .encryptAndSignPassphrase(encryptKey, signKey, cryptoContext)
    }

    @Suppress("UNUSED_PARAMETER")
    suspend operator fun invoke(
        userId: UserId,
        encryptKeys: List<KeyHolder>,
        signKey: KeyHolder,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<NestedPrivateKey> = coRunCatching(coroutineContext) {
        NestedPrivateKey
            .generateNestedPrivateKey(cryptoContext, DEFAULT_USERNAME, DEFAULT_DOMAIN, generatePassphrase::invoke)
            .encryptAndSignPassphrase(encryptKeys, signKey, cryptoContext)
    }

    suspend operator fun invoke(
        userId: UserId,
        encryptKey: KeyHolder,
        signatureAddress: String,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext
    ) =
        invoke(
            userId = userId,
            encryptKey = encryptKey,
            signKey = userAddressRepository.getAddressKeys(userId, signatureAddress),
            coroutineContext = coroutineContext,
        )

    companion object {
        private const val DEFAULT_USERNAME = "drive-key"
        private const val DEFAULT_DOMAIN = "proton.me"
    }
}
