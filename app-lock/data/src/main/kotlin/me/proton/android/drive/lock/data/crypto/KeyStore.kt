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

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.util.kotlin.CoreLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object Config {
    const val KEY_STORE_TYPE = "AndroidKeyStore"
    const val DEFAULT_CIPHER_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    const val DEFAULT_CIPHER_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    const val DEFAULT_CIPHER_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    const val DEFAULT_CIPHER_GCM_TAG_LENGTH = 128
    const val DEFAULT_CIPHER_IV_BYTES = 12
    const val DEFAULT_KEY_SIZE = 256
    const val DEFAULT_USER_AUTHENTICATION_REQUIRED = true
}

data class SecretKeyProperties(
    val keyAlias: String,
    val keyStoreType: String = Config.KEY_STORE_TYPE,
    val cipherAlgorithm: String = Config.DEFAULT_CIPHER_ALGORITHM,
    val cipherBlockMode: String = Config.DEFAULT_CIPHER_BLOCK_MODE,
    val cipherPadding: String = Config.DEFAULT_CIPHER_PADDING,
    val cipherKeySize: Int = Config.DEFAULT_KEY_SIZE,
    val userAuthenticationRequired: Boolean = Config.DEFAULT_USER_AUTHENTICATION_REQUIRED,
)

val SecretKeyProperties.transformation: String get() = "$cipherAlgorithm/$cipherBlockMode/$cipherPadding"

fun SecretKeyProperties.getOrCreateSecretKey(
    invalidateKeyByBiometricEnrollment: Boolean = true,
): SecretKey {
    val keyStore = KeyStore.getInstance(keyStoreType)
    keyStore.load(null)
    return if (keyStore.containsAlias(keyAlias)) {
        keyStore.getKey(keyAlias, null) as SecretKey
    } else {
        KeyGenerator.getInstance(
            cipherAlgorithm,
            keyStoreType,
        ).let { keyGenerator ->
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(cipherBlockMode)
                    .setEncryptionPaddings(cipherPadding)
                    .setKeySize(cipherKeySize)
                    .defaultKeyGenParameterSpecBuilder(
                        userAuthenticationRequired = userAuthenticationRequired,
                        invalidateKeyByBiometricEnrollment = invalidateKeyByBiometricEnrollment,
                    )
                    .build()
            )
            keyGenerator.generateKey()
        }
    }
}

fun KeyGenParameterSpec.Builder.defaultKeyGenParameterSpecBuilder(
    userAuthenticationRequired: Boolean,
    invalidateKeyByBiometricEnrollment: Boolean,
): KeyGenParameterSpec.Builder {
    setUserAuthenticationRequired(userAuthenticationRequired)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        setUserAuthenticationParameters(
            0,
            KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
        )
    }
    setInvalidatedByBiometricEnrollment(invalidateKeyByBiometricEnrollment)
    return this
}

fun SecretKeyProperties.getInitializedCipherForDecryption(
    initializationVector: ByteArray? = null,
    invalidateKeyByBiometricEnrollment: Boolean = true,
    cipherGcmTagLength: Int = Config.DEFAULT_CIPHER_GCM_TAG_LENGTH,
): Cipher = getCipher(transformation).apply {
    init(
        Cipher.DECRYPT_MODE,
        getOrCreateSecretKey(
            invalidateKeyByBiometricEnrollment = invalidateKeyByBiometricEnrollment,
        ),
        GCMParameterSpec(cipherGcmTagLength, initializationVector),
    )
}

fun SecretKeyProperties.getInitializedCipherForEncryption(
    invalidateKeyByBiometricEnrollment: Boolean = true,
): Cipher = getCipher(transformation).apply {
    init(
        Cipher.ENCRYPT_MODE,
        getOrCreateSecretKey(
            invalidateKeyByBiometricEnrollment = invalidateKeyByBiometricEnrollment,
        ),
    )
}

private fun getCipher(transformation: String): Cipher = Cipher.getInstance(transformation)

fun removeKey(keyProperties: SecretKeyProperties) {
    try {
        KeyStore.getInstance(keyProperties.keyStoreType).run {
            load(null)
            deleteEntry(keyProperties.keyAlias)
        }
    } catch (e: Exception) {
        CoreLogger.d(LogTag.DEFAULT, e, e.message.orEmpty())
    }
}
