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

import android.util.Base64
import me.proton.android.drive.lock.domain.entity.SecretKey
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.EncryptedString
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.keystore.use

class KeyStoreSecretKey(
    private val keyProperties: SecretKeyProperties,
    private val invalidateKeyByBiometricEnrollment: Boolean = true,
) : SecretKey {

    override fun encrypt(value: String): EncryptedString =
        value.encodeToByteArray().use { plainByteArray ->
            Base64.encodeToString(
                encrypt(plainByteArray).array,
                Base64.NO_WRAP,
            )
        }

    override fun encrypt(value: PlainByteArray): EncryptedByteArray {
        val cipher = keyProperties.getInitializedCipherForEncryption(invalidateKeyByBiometricEnrollment)
        val cipherByteArray = cipher.doFinal(value.array)
        return EncryptedByteArray(cipher.iv + cipherByteArray)
    }

    override fun decrypt(value: EncryptedString): String {
        val encryptedByteArray = Base64.decode(value, Base64.NO_WRAP)
        return decrypt(EncryptedByteArray(encryptedByteArray)).use { plainByteArray ->
            plainByteArray.array.decodeToString()
        }
    }

    override fun decrypt(value: EncryptedByteArray): PlainByteArray {
        val initializationVector = value.array.copyOf(Config.DEFAULT_CIPHER_IV_BYTES)
        val cipher = keyProperties.getInitializedCipherForDecryption(
            initializationVector = initializationVector,
            invalidateKeyByBiometricEnrollment = invalidateKeyByBiometricEnrollment,
        )
        val cipherByteArray = value.array.copyOfRange(Config.DEFAULT_CIPHER_IV_BYTES, value.array.size)
        return PlainByteArray(cipher.doFinal(cipherByteArray))
    }
}
