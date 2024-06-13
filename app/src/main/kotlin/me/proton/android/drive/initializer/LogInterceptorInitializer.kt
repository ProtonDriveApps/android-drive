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
import androidx.lifecycle.Lifecycle
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.usecase.AsyncAnnounceEvent
import me.proton.core.drive.log.data.interceptor.LogInterceptor
import me.proton.core.presentation.app.AppLifecycleProvider

class LogInterceptorInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with (
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                LogInterceptorInitializerEntryPoint::class.java,
            )
        ) {
            logInterceptor.announceEvent = announceEvent
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.STARTED)
                .onAccountReady { account ->
                    logInterceptor.userId = account.userId
                }
                .onAccountDisabled { account ->
                    logInterceptor.removeUserId(account.userId)
                }
                .onAccountRemoved { account ->
                    logInterceptor.removeUserId(account.userId)
                }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        AccountStateHandlerInitializer::class.java,
    )

    private fun LogInterceptor.removeUserId(userId: UserId) {
        if (this.userId == userId) {
            this.userId = null
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LogInterceptorInitializerEntryPoint {
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
        val announceEvent: AsyncAnnounceEvent
        val logInterceptor: LogInterceptor
    }
}
