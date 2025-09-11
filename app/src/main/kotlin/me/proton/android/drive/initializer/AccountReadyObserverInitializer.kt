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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.stats.ObserveApplicationState
import me.proton.android.drive.stats.ObserveWorkManager
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.drivelink.download.domain.manager.DownloadManager
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.util.kotlin.CoreLogger

class AccountReadyObserverInitializer : Initializer<Unit> {

    private val scopes = mutableMapOf<UserId, CoroutineScope>()

    override fun create(context: Context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            ApplicationObserverInitializerEntryPoint::class.java
        ).run {
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.CREATED)
                .onAccountReady { account ->
                    val userId = account.userId
                    val scope = scopes.getOrPut(userId) {
                        CoroutineScope(Dispatchers.IO + Job())
                    }
                    observeApplicationState().onEach { applicationState ->
                        val state = if (applicationState.inForeground) "foreground" else "background"
                        CoreLogger.d(
                            LogTag.DEFAULT,
                            "App in $state, ${applicationState.currentNetworkStatus?.replace("\n", ", ")}",
                        )
                        announceEvent(userId, applicationState)
                    }.launchIn(scope)
                    observeWorkManager(userId).onEach { workers ->
                        announceEvent(userId, workers)
                    }.launchIn(scope)
                    fileDownloader.start(userId, scope.coroutineContext)
                }
                .onAccountRemoved { account ->
                    fileDownloader.stop(account.userId)
                    scopes.remove(account.userId)?.cancel()
                }
        }

    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf()


    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ApplicationObserverInitializerEntryPoint {
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
        val observeApplicationState: ObserveApplicationState
        val observeWorkManager: ObserveWorkManager
        val announceEvent: AnnounceEvent
        val fileDownloader: DownloadManager.FileDownloader
    }
}
