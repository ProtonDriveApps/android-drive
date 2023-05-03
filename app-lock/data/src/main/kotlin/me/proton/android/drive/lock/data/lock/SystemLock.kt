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
package me.proton.android.drive.lock.data.lock

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.android.drive.lock.data.crypto.KeyStoreSecretKey
import me.proton.android.drive.lock.data.crypto.SecretKeyProperties
import me.proton.android.drive.lock.data.crypto.removeKey
import me.proton.android.drive.lock.data.provider.BiometricPromptProvider
import me.proton.android.drive.lock.domain.entity.AppLockType
import me.proton.android.drive.lock.domain.entity.LockKey
import me.proton.android.drive.lock.domain.entity.SecretKey
import me.proton.android.drive.lock.domain.lock.Lock
import me.proton.android.drive.lock.domain.lock.LockState
import me.proton.android.drive.lock.domain.repository.AppLockRepository
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

class SystemLock @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appLockRepository: AppLockRepository,
    private val biometricPromptProvider: BiometricPromptProvider,
) : Lock {
    private val keyProperties = SecretKeyProperties(
        keyAlias = SYSTEM_KEY_ALIAS,
        userAuthenticationRequired = false,
    )
    private val secretKey: SecretKey = KeyStoreSecretKey(keyProperties)

    override suspend fun <T> unlock(
        key: String,
        block: suspend (passphrase: ByteArray) -> T,
    ): Result<T> = coRunCatching {
        val lockKey = appLockRepository.getLockKey(AppLockType.SYSTEM)
        require(lockKey.appKey == key)
        biometricPromptProvider.authenticate(
            title = appContext.getString(I18N.string.app_lock_biometric_title_app_locked),
            subtitle = appContext.getString(I18N.string.app_lock_biometric_subtitle_app_locked),
            cryptoObject = null,
        ).getOrThrow()
        block(secretKey.decrypt(EncryptedByteArray(lockKey.appKeyPassphrase)).array)
    }

    override suspend fun lock(passphrase: ByteArray): Result<ByteArray> = coRunCatching {
        biometricPromptProvider.authenticate(
            title = appContext.getString(I18N.string.app_lock_biometric_title_confirmation),
            subtitle = appContext.getString(I18N.string.app_lock_biometric_subtitle_confirmation_enable),
            cryptoObject = null,
        ).getOrThrow()
        secretKey.encrypt(PlainByteArray(passphrase)).array
    }

    override suspend fun disable(userAuthenticationRequired: Boolean) {
        if (userAuthenticationRequired) {
            biometricPromptProvider.authenticate(
                title = appContext.getString(I18N.string.app_lock_biometric_title_confirmation),
                subtitle = appContext.getString(
                    I18N.string.app_lock_biometric_subtitle_confirmation_disable
                ),
                cryptoObject = null,
            ).getOrThrow()
        }
        appLockRepository.deleteLockKey(AppLockType.SYSTEM)
        removeKey(keyProperties)
    }

    override suspend fun enable(lockKey: LockKey) {
        appLockRepository.insertLockKey(lockKey)
    }

    override fun getLockState(): LockState = biometricPromptProvider.getLockState()

    companion object {
        private const val SYSTEM_KEY_ALIAS = "system_lock_key"
    }
}
