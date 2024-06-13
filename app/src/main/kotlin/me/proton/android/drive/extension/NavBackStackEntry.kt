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

package me.proton.android.drive.extension

import android.os.Build
import android.os.Bundle
import androidx.navigation.NavBackStackEntry
import java.io.Serializable

fun NavBackStackEntry.requireArguments() = requireNotNull(arguments) { "arguments bundle is null" }

inline fun <reified T> NavBackStackEntry.require(key: String): T {
    return requireNotNull(get(key)) {
        "$key is required"
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> NavBackStackEntry.get(key: String): T? {
    val bundleArgs = requireArguments()
    return when (T::class) {
        Boolean::class -> bundleArgs.getBoolean(key)
        Int::class -> bundleArgs.getInt(key)
        Long::class -> bundleArgs.getLong(key)
        Float::class -> bundleArgs.getFloat(key)
        Double::class -> bundleArgs.getDouble(key)
        String::class -> bundleArgs.getString(key)
        else -> bundleArgs.getString(key)
    } as T?
}

fun <T : Serializable> NavBackStackEntry.requireSerializable(
    key: String,
    clazz: Class<T>,
): T {
    return requireNotNull(getSerializable(key, clazz)) {
        "$key is required"
    }
}

fun <T : Serializable> NavBackStackEntry.getSerializable(
    key: String,
    clazz: Class<T>,
): T? {
    val bundleArgs = requireArguments()

    return bundleArgs.getSerializableCompat(key, clazz)
}

@Suppress("DEPRECATION", "UNCHECKED_CAST")
fun <T : Serializable> Bundle.getSerializableCompat(key: String, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializable(key, clazz)
    } else {
        getSerializable(key) as T?
    }

