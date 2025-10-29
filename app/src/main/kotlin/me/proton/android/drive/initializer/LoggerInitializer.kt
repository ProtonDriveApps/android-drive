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
import android.os.StrictMode
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.proton.android.drive.BuildConfig
import me.proton.android.drive.log.DriveLogTag
import me.proton.android.drive.log.DriveLogger
import me.proton.android.drive.log.UserLogger
import me.proton.android.drive.usecase.GetFileLoggerTree
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.usersettings.domain.UsersSettingsHandler
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
        val userLogger = entryPoint.userLogger()
        entryPoint.userSettingsHandler().onUsersSettingsChanged(
            merge = { usersSettings ->
                usersSettings.none { userSettings -> userSettings?.crashReports == false }
            }
        ) { crashReports ->
            CoreLogger.set(
                logger = if (crashReports) logger else userLogger
            )
        }
        if (BuildConfig.DEBUG || BuildConfig.FLAVOR == BuildConfig.FLAVOR_ALPHA) {
            Timber.plant(Timber.DebugTree())
            if (entryPoint.configurationProvider().logToFileInDebugEnabled) {
                entryPoint.getFileLoggerTree().invoke()
                    .onSuccess { fileLoggerTree ->
                        Timber.plant(fileLoggerTree)
                    }
            } else {
                strictMode(logger)
            }
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
        threadPolicyBuilder.penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
            logger.e(DriveLogTag.STRICT_MODE, violation)
        }
        vmPolicyBuilder.penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
            logger.e(DriveLogTag.STRICT_MODE, violation)
        }
        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LoggerInitializerEntryPoint {
        fun driveLogger(): DriveLogger
        fun userLogger(): UserLogger
        fun userSettingsHandler(): UsersSettingsHandler
        fun configurationProvider(): ConfigurationProvider
        fun getFileLoggerTree(): GetFileLoggerTree
    }
}
