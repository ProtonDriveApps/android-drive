/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.crypto.data.usecase

import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.crypto.common.pgp.exception.CryptoException
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.entity.CipherSpec
import me.proton.core.drive.crypto.domain.extension.secretKey
import me.proton.core.drive.crypto.domain.usecase.DecryptData
import javax.crypto.Cipher
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class DecryptDataImpl @Inject constructor() : DecryptData {

    override suspend fun invoke(
        decryptKey: SessionKey,
        input: String,
        cipherSpec: CipherSpec,
        coroutineContext: CoroutineContext
    ): Result<ByteArray> = coRunCatching(coroutineContext) {
        val combined = Base64.decode(input)
        val iv = combined.copyOfRange(0, cipherSpec.ivSize.value.toInt())
        val cipherText = combined.copyOfRange(cipherSpec.ivSize.value.toInt(), combined.size)
        val cipher = Cipher.getInstance(cipherSpec.transformation.value)
        val key = decryptKey.secretKey(cipherSpec.transformation)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            cipherSpec.algorithmParameterSpec(iv),
        )
        cipher.doFinal(cipherText)
    }.recoverCatching { throwable ->
        throw CryptoException(throwable)
    }
}
