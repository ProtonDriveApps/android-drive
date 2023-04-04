/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.android.drive.lock.data.crypto

import me.proton.android.drive.lock.domain.entity.SecretKey
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.EncryptedString
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.crypto.common.pgp.Unarmored

class PgpSecretKey(
    passphrase: PlainByteArray,
    val lockedKey: Armored,
    cryptoContext: CryptoContext,
) : SecretKey {
    private val pgpCrypto = cryptoContext.pgpCrypto
    private val unlockedKey: Unarmored
    private val publicKey: Armored

    init {
        passphrase.use {
            unlockedKey = pgpCrypto.unlock(lockedKey, passphrase.array).value
            publicKey = pgpCrypto.getPublicKey(lockedKey)
        }
    }

    override fun encrypt(value: String): EncryptedString = pgpCrypto.encryptText(value, publicKey)

    override fun decrypt(value: EncryptedString): String = pgpCrypto.decryptText(value, unlockedKey)

    override fun encrypt(value: PlainByteArray): EncryptedByteArray =
        EncryptedByteArray(pgpCrypto.encryptData(value.array, publicKey).toByteArray())

    override fun decrypt(value: EncryptedByteArray): PlainByteArray =
        PlainByteArray(pgpCrypto.decryptData(pgpCrypto.getArmored(value.array), unlockedKey))
}
