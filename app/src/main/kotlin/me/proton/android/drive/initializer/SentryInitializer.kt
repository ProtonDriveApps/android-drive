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
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import me.proton.android.drive.BuildConfig
import me.proton.core.usersettings.domain.UsersSettingsHandler
import me.proton.core.util.android.sentry.TimberLoggerIntegration
import me.proton.core.util.android.sentry.project.AccountSentryHubBuilder

class SentryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SentryInitializerEntryPoint::class.java
        )
        var isCrashReportEnabled = true
        entryPoint.usersSettingsHandler().onUsersSettingsChanged(
            merge = { usersSettings ->
                usersSettings.none { userSettings -> userSettings?.crashReports == false }
            }
        ) { crashReports ->
            isCrashReportEnabled = crashReports
        }
        val beforeSendCallback = SentryOptions.BeforeSendCallback { event, _ ->
            if (isCrashReportEnabled) event else null
        }
        SentryAndroid.init(context) { options ->
            options.dsn = BuildConfig.SENTRY_DSN.takeIf { !BuildConfig.DEBUG }.orEmpty()
            options.release = BuildConfig.VERSION_NAME
            options.isEnableAutoSessionTracking = true
            options.environment = BuildConfig.FLAVOR
            options.beforeSend = beforeSendCallback
            options.addIntegration(
                TimberLoggerIntegration(
                    minEventLevel = SentryLevel.ERROR,
                    minBreadcrumbLevel = SentryLevel.DEBUG
                )
            )
        }

        entryPoint.accountSentryHubBuilder().invoke(
            sentryDsn = BuildConfig.ACCOUNT_SENTRY_DSN.takeIf { !BuildConfig.DEBUG }.orEmpty()
        ) { options ->
            options.beforeSend = beforeSendCallback
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SentryInitializerEntryPoint {
        fun accountSentryHubBuilder(): AccountSentryHubBuilder
        fun usersSettingsHandler(): UsersSettingsHandler
    }
}
