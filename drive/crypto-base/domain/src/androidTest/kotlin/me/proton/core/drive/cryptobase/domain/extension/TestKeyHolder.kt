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

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.keystore.encrypt
import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.key.domain.entity.key.KeyId
import me.proton.core.key.domain.entity.key.PrivateKey
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.entity.keyholder.KeyHolderPrivateKey

class TestKeyHolder(
    private val context: CryptoContext,
    private val privateKeyArmored: Armored,
    private val privateKeyPassphrase: ByteArray
) : KeyHolder {

    inner class TestKeyHolderPrivateKey(id: String, isPrimary: Boolean) : KeyHolderPrivateKey {
        override val keyId: KeyId = KeyId(id)
        override val privateKey: PrivateKey = PrivateKey(
            key = privateKeyArmored,
            isPrimary = isPrimary,
            // Encrypt passphrase as it should be stored in PrivateKey.
            passphrase = PlainByteArray(privateKeyPassphrase).encrypt(context.keyStoreCrypto)
        )
    }

    override val keys: List<KeyHolderPrivateKey> = listOf(
        TestKeyHolderPrivateKey(
            id = privateKeyArmored.hashCode().toString(),
            isPrimary = true
        )
    )
}
