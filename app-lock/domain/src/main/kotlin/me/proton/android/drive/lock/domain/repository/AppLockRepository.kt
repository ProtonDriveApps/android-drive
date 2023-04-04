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
package me.proton.android.drive.lock.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.android.drive.lock.domain.entity.AppLock
import me.proton.android.drive.lock.domain.entity.AppLockType
import me.proton.android.drive.lock.domain.entity.LockKey
import kotlin.time.Duration

interface AppLockRepository {
    fun hasAppLockKeyFlow(): Flow<Boolean>
    suspend fun hasAppLockKey(): Boolean
    suspend fun getAppLockKey(): AppLock
    suspend fun insertAppLockKey(appLock: AppLock)
    suspend fun deleteAppLockKey()

    suspend fun hasLockKey(lockType: AppLockType): Boolean
    suspend fun getLockKey(lockType: AppLockType): LockKey
    suspend fun insertLockKey(lockKey: LockKey)
    suspend fun deleteLockKey(lockType: AppLockType)

    suspend fun hasAutoLockDuration(): Boolean
    fun getAutoLockDuration(): Flow<Duration>
    suspend fun insertOrUpdateAutoLockDuration(duration: Duration)

    suspend fun hasEnableAppLockTimestamp(): Boolean
    suspend fun insertOrUpdateEnableAppLockTimestamp(timestamp: Long)
}
