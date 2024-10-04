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

import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.drive.crypto.domain.entity.CipherSpec
import me.proton.core.test.kotlin.assertEquals
import org.junit.Test
import java.security.SecureRandom

class EncryptAndDecryptDataTest {
    private val encryptData = EncryptDataImpl()
    private val decryptData = DecryptDataImpl()
    private val sessionKey: SessionKey = SessionKey(
        key = let {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            bytes
        }
    )

    @Test
    fun `happy path`() = runTest {
        // Given
        val message = "Hello World"

        // When
        val encryptedMessage = encryptData(
            encryptKey = sessionKey,
            cipherSpec = CipherSpec.AES_GCM_NO_PADDING_IV_16_BYTES,
            input = message.toByteArray(),
        ).getOrThrow()

        val decryptedMessage = String(
            decryptData(
                decryptKey = sessionKey,
                cipherSpec = CipherSpec.AES_GCM_NO_PADDING_IV_16_BYTES,
                input = encryptedMessage,
            ).getOrThrow()
        )

        // Then
        assertEquals(message, decryptedMessage) {
            "Decrypted message ($decryptedMessage) does not match original message"
        }
    }
}
