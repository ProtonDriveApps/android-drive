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
package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.crypto.common.pgp.EncryptedMessage
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.entity.UnlockedKey
import me.proton.core.drive.cryptobase.domain.usecase.EncryptData
import me.proton.core.drive.cryptobase.domain.usecase.GetUnarmored
import me.proton.core.drive.cryptobase.domain.usecase.SignData
import me.proton.core.drive.cryptobase.domain.usecase.SignFile
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.extension.keyHolder
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class EncryptedSignature @Inject constructor(
    private val getUnarmored: GetUnarmored,
    private val encryptData: EncryptData,
    private val signFile: SignFile,
    private val signData: SignData,
) {
    internal suspend operator fun invoke(
        unlockedEncryptKey: UnlockedKey,
        unlockedSignKey: UnlockedKey,
        file: File,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecryptWithIO.coroutineContext,
    ): Result<EncryptedMessage> = coRunCatching(coroutineContext) {
        encryptData(
            unlockedKey = unlockedEncryptKey,
            input = getUnarmored(
                data = signFile(unlockedSignKey, file).getOrThrow(),
                coroutineContext = coroutineContext,
            ).getOrThrow()
        ).getOrThrow()
    }

    suspend operator fun invoke(
        encryptKey: Key.Node,
        signKey: Key,
        file: File,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecryptWithIO.coroutineContext,
    ): Result<EncryptedMessage> = coRunCatching(coroutineContext) {
        encryptData(
            encryptKey = encryptKey.keyHolder,
            input = getUnarmored(
                data = signFile(signKey.keyHolder, file).getOrThrow(),
                coroutineContext = coroutineContext,
            ).getOrThrow()
        ).getOrThrow()
    }

    suspend operator fun invoke(
        unlockedEncryptKey: UnlockedKey,
        unlockedSignKey: UnlockedKey,
        input: ByteArray,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecryptWithIO.coroutineContext,
    ): Result<EncryptedMessage> = coRunCatching(coroutineContext) {
        encryptData(
            unlockedKey = unlockedEncryptKey,
            input = getUnarmored(
                data = signData(unlockedSignKey, input).getOrThrow(),
                coroutineContext = coroutineContext,
            ).getOrThrow()
        ).getOrThrow()
    }

    suspend operator fun invoke(
        encryptKey: Key.Node,
        signKey: Key,
        input: ByteArray,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecryptWithIO.coroutineContext,
    ): Result<EncryptedMessage> = coRunCatching(coroutineContext) {
        encryptData(
            encryptKey = encryptKey.keyHolder,
            input = getUnarmored(
                data = signData(signKey.keyHolder, input).getOrThrow(),
                coroutineContext = coroutineContext,
            ).getOrThrow()
        ).getOrThrow()
    }
}
