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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.extension.log
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.download.data.handler.ObservabilityDownloadErrorHandler
import me.proton.core.drive.drivelink.download.domain.manager.DownloadErrorManager
import me.proton.core.presentation.app.AppLifecycleProvider

class DownloadInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with (
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                DownloadInitializerEntryPoint::class.java,
            )
        ) {
            downloadErrorManager.errors
                .onEach { downloadError -> handleDownloadError(downloadError) }
                .launchIn(appLifecycleProvider.lifecycle.coroutineScope)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        WorkManagerInitializer::class.java,
    )

    private suspend fun DownloadInitializerEntryPoint.handleDownloadError(
        downloadError: DownloadErrorManager.Error
    ) {
        coRunCatching {
            downloadErrorHandler.onError(downloadError)
        }.onFailure { error ->
            error.log(LogTag.DOWNLOAD, "Failed to handle download error")
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadInitializerEntryPoint {
        val appLifecycleProvider: AppLifecycleProvider
        val downloadErrorManager: DownloadErrorManager
        val downloadErrorHandler: ObservabilityDownloadErrorHandler
    }
}
