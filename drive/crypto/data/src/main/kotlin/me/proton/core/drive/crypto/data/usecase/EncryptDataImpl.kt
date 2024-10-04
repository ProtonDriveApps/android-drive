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
import me.proton.core.drive.crypto.domain.usecase.EncryptData
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class EncryptDataImpl @Inject constructor() : EncryptData {

    override suspend fun invoke(
        encryptKey: SessionKey,
        input: ByteArray,
        cipherSpec: CipherSpec,
        coroutineContext: CoroutineContext,
    ): Result<String> = coRunCatching(coroutineContext) {
        val cipher = Cipher.getInstance(cipherSpec.transformation.value)
        val key = encryptKey.secretKey(cipherSpec.transformation)
        val iv = getIv(cipherSpec.ivSize.value.toInt())
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            cipherSpec.algorithmParameterSpec(iv),
        )
        val cipherText = cipher.doFinal(input)
        val combined = cipher.iv + cipherText
        Base64.encode(combined)
    }.recoverCatching { throwable ->
        throw CryptoException(throwable)
    }

    private fun getIv(size: Int): ByteArray = ByteArray(size).apply {
        SecureRandom().nextBytes(this)
    }
}
