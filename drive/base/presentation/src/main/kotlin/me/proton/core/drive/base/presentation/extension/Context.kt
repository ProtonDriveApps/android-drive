/*
 * Copyright (c) 2021-2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.core.drive.base.presentation.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.PluralsRes
import java.util.Locale

@Suppress("DEPRECATION")
val Context.currentLocale: Locale
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales[0]
    } else {
        resources.configuration.locale
    }

fun Context.quantityString(@PluralsRes pluralRes: Int, quantity: Int): String =
    resources.getQuantityString(
        pluralRes,
        quantity,
        quantity,
    )

fun Context.launchApplicationDetailsSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts(Scheme.PACKAGE, packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    )
}

fun Context.launchIgnoreBatteryOptimizations(isIgnoringBatteryOptimizations: Boolean = false) {
    startActivity(Intent().apply {
        if (isIgnoringBatteryOptimizations) {
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        } else {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:$packageName")
        }
    })
}

private object Scheme {
    const val PACKAGE = "package"
}
