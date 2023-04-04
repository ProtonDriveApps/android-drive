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
package me.proton.android.drive.lock.domain.manager

import kotlinx.coroutines.flow.StateFlow
import me.proton.android.drive.lock.domain.entity.AppLock
import me.proton.android.drive.lock.domain.entity.SecretKey

interface AppLockManager {
    val locked: StateFlow<Boolean>
    val enabled: StateFlow<Boolean>
    suspend fun isLocked(): Boolean
    suspend fun isEnabled(): Boolean
    suspend fun unlock(appKey: SecretKey)
    suspend fun lock()
    suspend fun enable(secretKey: SecretKey, appLock: AppLock): Result<Boolean>
    suspend fun disable(): Result<Boolean>
}
