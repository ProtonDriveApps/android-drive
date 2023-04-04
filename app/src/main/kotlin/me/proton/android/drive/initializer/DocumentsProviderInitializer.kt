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
import androidx.lifecycle.coroutineScope
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.lock.domain.manager.AppLockManager
import me.proton.core.drive.documentsprovider.data.DriveDocumentsProvider
import me.proton.core.presentation.app.AppLifecycleProvider

class DocumentsProviderInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        with (
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                AutoLockInitializerEntryPoint::class.java
            )
        ) {
            appLockManager.enabled
                .onEach {
                    DriveDocumentsProvider.notifyRootsHaveChanged(context)
                }
                .launchIn(appLifecycleProvider.lifecycle.coroutineScope)
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        LoggerInitializer::class.java,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AutoLockInitializerEntryPoint {
        val appLifecycleProvider: AppLifecycleProvider
        val appLockManager: AppLockManager
    }
}
