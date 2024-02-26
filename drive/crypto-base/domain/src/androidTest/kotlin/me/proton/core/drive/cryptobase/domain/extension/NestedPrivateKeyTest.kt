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

package me.proton.core.drive.cryptobase.domain.extension

import me.proton.core.crypto.android.context.AndroidCryptoContext
import me.proton.core.crypto.common.keystore.decrypt
import me.proton.core.key.domain.decryptNestedKeyOrThrow
import me.proton.core.key.domain.entity.key.NestedPrivateKey
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.useKeys
import org.junit.Assert
import org.junit.Test

class NestedPrivateKeyTest {
    private val context = AndroidCryptoContext()
    private val keyHolder1: KeyHolder = TestKeyHolder(context, TestKeys.Key1.privateKey, TestKeys.Key1.passphrase)
    private val keyHolder2: KeyHolder = TestKeyHolder(context, TestKeys.Key2.privateKey, TestKeys.Key2.passphrase)
    private val passphrase = byteArrayOf(
        0, 127, 90, 86, 43, 50, 57, 109, 100, 49, 72, 100, 55, 112, 85, 107, 121, 87, 80, 121, 50, 57, 71, 104, 89, 107,
        98, 69, 56, 104, 106, 112
    )

    @Test
    fun nested_private_key_encrypted_with_two_keys_can_be_decrypted_by_both() {
        // When
        val key = NestedPrivateKey
            .generateNestedPrivateKey(
                context = context,
                username = "user",
                domain = "example.proton.me",
            ) { passphrase.clone() }
            .encryptAndSignPassphrase(encryptKeys = listOf(keyHolder1, keyHolder2), keyHolder1, context)

        // Then
        listOf(keyHolder1, keyHolder2).forEach { keyHolder ->
            val decryptedKey = keyHolder.useKeys(context) {
                decryptNestedKeyOrThrow(key)
            }
            Assert.assertArrayEquals(
                passphrase,
                requireNotNull(decryptedKey.privateKey.passphrase).decrypt(context.keyStoreCrypto).array,
            )
        }
    }
}
