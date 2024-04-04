/*
 * Copyright (c) 2024 Proton AG.
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
import androidx.lifecycle.coroutineScope
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.extension.log
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.data.domain.usecase.PingActiveUser
import me.proton.core.presentation.app.AppLifecycleProvider

class PingActiveUserInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        with (
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                PingActiveUserInitializerEntryPoint::class.java
            )
        ) {
            combine(
                appLifecycleProvider.state,
                accountManager.getPrimaryUserId(),
            ) { state, primaryUserId ->
                takeIf { state == AppLifecycleProvider.State.Foreground }
                    ?.let {
                        primaryUserId
                    }
            }
                .distinctUntilChanged()
                .filterNotNull()
                .onEach { primaryUserId ->
                    pingActiveUser(primaryUserId)
                        .onFailure { error ->
                            error.log(LogTag.TELEMETRY, "Ping active user failed")
                        }
                }
                .launchIn(appLifecycleProvider.lifecycle.coroutineScope)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        LoggerInitializer::class.java,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PingActiveUserInitializerEntryPoint {
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
        val pingActiveUser: PingActiveUser
    }
}
