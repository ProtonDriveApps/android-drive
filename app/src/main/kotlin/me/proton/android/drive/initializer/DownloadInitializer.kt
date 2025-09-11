/*
 * Copyright (c) 2025 Proton AG.
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.LogTag.TRACKING
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.download.domain.handler.DownloadErrorHandler
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.drive.linkdownload.domain.manager.DownloadSpeedManager
import me.proton.core.drive.linkdownload.domain.usecase.IsDownloading
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.util.kotlin.CoreLogger

class DownloadInitializer : Initializer<Unit> {

    private val scopes = mutableMapOf<UserId, CoroutineScope>()

    override fun create(context: Context) {
        with(
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                DownloadInitializerEntryPoint::class.java,
            )
        ) {
            downloadErrorManager.errors
                .onEach { downloadError -> handleDownloadError(downloadError) }
                .launchIn(appLifecycleProvider.lifecycle.coroutineScope)

            accountManager.observe(
                appLifecycleProvider.lifecycle,
                androidx.lifecycle.Lifecycle.State.CREATED
            )
                .onAccountReady { account ->
                    val userId = account.userId
                    val scope = scopes.getOrPut(userId) {
                        CoroutineScope(Dispatchers.IO + Job())
                    }
                    isDownloading(userId).onEach { downloading ->
                        if (downloading) {
                            CoreLogger.v(TRACKING, "Resuming, downloading files")
                            downloadSpeedManager.resume()
                        } else {
                            CoreLogger.v(TRACKING, "Pausing, all downloads are done")
                            downloadSpeedManager.pause(userId)
                        }
                    }.launchIn(scope)
                }
                .onAccountRemoved { account ->
                    scopes.remove(account.userId)?.cancel()
                }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        WorkManagerInitializer::class.java,
    )

    private suspend fun DownloadInitializerEntryPoint.handleDownloadError(
        downloadError: DownloadErrorManager.Error
    ) {
        downloadErrorHandlers.forEach { handler ->
            coRunCatching {
                handler.onError(downloadError)
            }.getOrNull(LogTag.DOWNLOAD, "Failed to handle download error")
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadInitializerEntryPoint {
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
        val downloadErrorManager: DownloadErrorManager
        val downloadSpeedManager: DownloadSpeedManager
        val isDownloading: IsDownloading
        val downloadErrorHandlers: @JvmSuppressWildcards Set<DownloadErrorHandler>
    }
}
