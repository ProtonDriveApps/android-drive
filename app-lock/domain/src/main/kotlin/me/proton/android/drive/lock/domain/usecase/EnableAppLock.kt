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
package me.proton.android.drive.lock.domain.usecase

import me.proton.android.drive.lock.domain.entity.AppLockType
import me.proton.android.drive.lock.domain.entity.LockKey
import me.proton.android.drive.lock.domain.lock.Lock
import me.proton.android.drive.lock.domain.manager.AppLockManager
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.GeneratePassphrase
import javax.inject.Inject

class EnableAppLock @Inject constructor(
    private val generatePassphrase: GeneratePassphrase,
    private val generateSecretKey: GenerateSecretKey,
    private val getAppLock: GetAppLock,
    private val appLockManager: AppLockManager,
    private val locks: @JvmSuppressWildcards Map<AppLockType, Lock>,
) {

    suspend operator fun invoke(lockType: AppLockType = AppLockType.SYSTEM) = coRunCatching {
        val passphrase = generatePassphrase()
        val secretKey = generateSecretKey(passphrase).getOrThrow()
        val appLock = getAppLock(secretKey, lockType).getOrThrow()
        val lock = requireNotNull(locks[lockType])
        val lockKey = LockKey(
            appKeyPassphrase = lock.lock(passphrase).getOrThrow(),
            appKey = appLock.key,
            type = appLock.type,
        )
        appLockManager.enable(secretKey, appLock).getOrThrow()
        lock.enable(lockKey)
    }
}
