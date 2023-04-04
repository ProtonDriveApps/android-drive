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

package me.proton.android.drive.lock.domain.lock

import me.proton.android.drive.lock.domain.entity.LockKey

interface Lock {
    /**
     * Unlocks passphrase for a given [key] and provides it to the [block].
     * After [block] is done, passphrase should not be available anymore.
     */
    suspend fun<T> unlock(key: String, block: suspend (passphrase: ByteArray) -> T): Result<T>

    /**
     * Locks [passphrase] so that it's safe to store.
     */
    suspend fun lock(passphrase: ByteArray): Result<ByteArray>

    /**
     * Called when [Lock] should not be used anymore. If [userAuthenticationRequired] is true then user authentication
     * is required before disabling can be done.
     */
    suspend fun disable(userAuthenticationRequired: Boolean)

    /**
     * Called when [Lock] should protect [lockKey]
     */
    suspend fun enable(lockKey: LockKey)

    /**
     * Provides current [Lock] state. See [LockState].
     */
    fun getLockState(): LockState
}

sealed interface LockState {
    object NotAvailable : LockState
    object SetupRequired : LockState
    object Ready : LockState
}
