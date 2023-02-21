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
import android.os.Build
import android.os.StrictMode
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.log.DriveLogger
import me.proton.android.drive.log.NoOpLogger
import me.proton.android.drive.log.deviceInfo
import me.proton.core.usersettings.domain.DeviceSettingsHandler
import me.proton.core.usersettings.domain.onDeviceSettingsChanged
import me.proton.core.util.kotlin.CoreLogger
import timber.log.Timber
import java.util.concurrent.Executors

class LoggerInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            LoggerInitializerEntryPoint::class.java
        )
        val logger = entryPoint.driveLogger()
        val handler = entryPoint.deviceSettingsHandler()

        handler.onDeviceSettingsChanged { deviceSettings ->
            CoreLogger.set(
                logger = if (deviceSettings.isCrashReportEnabled) logger else NoOpLogger()
            )
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            strictMode(logger)
        }
        CoreLogger.set(logger)
        logger.deviceInfo()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(SentryInitializer::class.java)

    private fun strictMode(logger: DriveLogger) {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
            .detectAll()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
            .detectAll()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            threadPolicyBuilder.penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                logger.e(DriveLogTag.STRICT_MODE, violation)
            }
            vmPolicyBuilder.penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                logger.e(DriveLogTag.STRICT_MODE, violation)
            }
        } else {
            threadPolicyBuilder.penaltyLog()
            vmPolicyBuilder.penaltyLog()
        }
        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LoggerInitializerEntryPoint {
        fun driveLogger(): DriveLogger
        fun deviceSettingsHandler(): DeviceSettingsHandler
    }
}
