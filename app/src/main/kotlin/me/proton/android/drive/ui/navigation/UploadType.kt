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

package me.proton.android.drive.ui.navigation

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object UploadType : NavType<UploadParameters>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): UploadParameters? {
        return getParcelable(bundle, key, UploadParameters::class.java)
    }

    override fun parseValue(value: String): UploadParameters {
        return Json.decodeFromString(value)
    }

    override fun put(bundle: Bundle, key: String, value: UploadParameters) {
        bundle.putParcelable(key, value)
    }
}

@Suppress("UNCHECKED_CAST", "DEPRECATION")
inline fun <reified T : Parcelable> getParcelable(bundle: Bundle, key: String, clazz: Class<T>) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        bundle.getParcelable(key, clazz)
    } else {
        bundle.getParcelable(key)
    }
