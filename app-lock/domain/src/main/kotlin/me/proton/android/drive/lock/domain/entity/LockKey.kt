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
package me.proton.android.drive.lock.domain.entity

data class LockKey(
    val appKeyPassphrase: ByteArray,
    val appKey: String,
    val type: AppLockType,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LockKey

        if (!appKeyPassphrase.contentEquals(other.appKeyPassphrase)) return false
        if (appKey != other.appKey) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = appKeyPassphrase.contentHashCode()
        result = 31 * result + appKey.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}
