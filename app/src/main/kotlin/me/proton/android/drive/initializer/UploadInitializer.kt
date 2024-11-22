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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.extension.log
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.upload.domain.handler.UploadErrorHandler
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.presentation.app.AppLifecycleProvider

class UploadInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with (
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                UploadInitializerEntryPoint::class.java,
            )
        ) {
            uploadErrorManager.errors
                .onEach { uploadError -> handleUploadError(uploadError) }
                .launchIn(appLifecycleProvider.lifecycle.coroutineScope)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        WorkManagerInitializer::class.java,
    )

    private suspend fun UploadInitializerEntryPoint.handleUploadError(uploadError: UploadErrorManager.Error) {
        uploadErrorHandlers.forEach { uploadErrorHandler ->
            runCatching {
                uploadErrorHandler.onError(uploadError)
            }.onFailure { error ->
                error.log(LogTag.UPLOAD, "Failed to handle upload error")
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UploadInitializerEntryPoint {
        val accountManager: AccountManager
        val appLifecycleProvider: AppLifecycleProvider
        val uploadErrorManager: UploadErrorManager
        val uploadErrorHandlers: @JvmSuppressWildcards Set<UploadErrorHandler>
    }
}
