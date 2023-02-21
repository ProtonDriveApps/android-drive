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

import me.proton.core.crypto.common.pgp.EncryptedMessage
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.entity.UnlockedKey
import me.proton.core.drive.cryptobase.domain.extension.withUnlockedKey
import me.proton.core.key.domain.encryptData
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EncryptData @Inject constructor(
    private val unlockKey: UnlockKey,
) {
    suspend operator fun invoke(
        encryptKey: KeyHolder,
        input: ByteArray,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<EncryptedMessage> = unlockKey(encryptKey, coroutineContext) { key ->
        invoke(key, input, coroutineContext).getOrThrow()
    }

    suspend operator fun invoke(
        unlockedKey: UnlockedKey,
        input: ByteArray,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<EncryptedMessage> = coRunCatching(coroutineContext) {
        withUnlockedKey(unlockedKey) { key ->
            key.encryptData(input)
        }
    }
}
