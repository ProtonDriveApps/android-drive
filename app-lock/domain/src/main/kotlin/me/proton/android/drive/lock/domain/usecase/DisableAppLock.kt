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
import me.proton.android.drive.lock.domain.lock.Lock
import me.proton.android.drive.lock.domain.manager.AppLockManager
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class DisableAppLock @Inject constructor(
    private val appLockManager: AppLockManager,
    private val locks: @JvmSuppressWildcards Map<AppLockType, Lock>,
) {
    suspend operator fun invoke(
        lockType: AppLockType = AppLockType.SYSTEM,
        userAuthenticationRequired: Boolean = true,
    ): Result<Unit> = coRunCatching {
        if (appLockManager.isEnabled()) {
            requireNotNull(locks[lockType]).disable(userAuthenticationRequired)
            appLockManager.disable().getOrThrow()
        }
    }
}
