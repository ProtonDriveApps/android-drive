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

package me.proton.android.drive.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import me.proton.android.drive.BuildConfig
import me.proton.core.usersettings.domain.DeviceSettingsHandler
import me.proton.core.usersettings.domain.onDeviceSettingsChanged

class SentryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SentryInitializerEntryPoint::class.java
        )
        var isCrashReportEnabled = true
        entryPoint.deviceSettingsHandler().onDeviceSettingsChanged { deviceSettings ->
            isCrashReportEnabled = deviceSettings.isCrashReportEnabled
        }
        SentryAndroid.init(context) { options ->
            options.dsn = BuildConfig.SENTRY_DSN.takeIf { !BuildConfig.DEBUG }.orEmpty()
            options.release = BuildConfig.VERSION_NAME
            options.isEnableAutoSessionTracking = false
            options.environment = BuildConfig.FLAVOR
            options.beforeSend = SentryOptions.BeforeSendCallback { event, hint ->
                if (isCrashReportEnabled) event else null
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SentryInitializerEntryPoint {
        fun deviceSettingsHandler(): DeviceSettingsHandler
    }
}
