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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.drivelink.photo.domain.manager.PhotoShareMigrationManager
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.util.kotlin.CoreLogger

class PhotoShareMigrationManagerInitializer : Initializer<Unit> {
    private val scopes = mutableMapOf<UserId, CoroutineScope>()

    override fun create(context: Context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PhotoShareMigrationManagerInitializerEntryPoint::class.java
        ).run {
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.RESUMED)
                .onAccountReady { account ->
                    val userId = account.userId
                    val scope = scopes.getOrPut(userId) {
                        CoroutineScope(Dispatchers.IO + Job())
                    }
                    photoShareMigrationManager.initialize(
                        userId,
                        scope,
                        appLifecycleProvider.state.map { state -> state == AppLifecycleProvider.State.Foreground },
                    )
                    photoShareMigrationManager
                        .status
                        .onEach { status ->
                            CoreLogger.d(LogTag.PHOTO, "On migration status: ${status.name}")
                        }
                        .launchIn(scope)
                }
                .onAccountRemoved { account ->
                    scopes.remove(account.userId)?.cancel()
                }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        LoggerInitializer::class.java,
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PhotoShareMigrationManagerInitializerEntryPoint {
    val accountManager: AccountManager
    val appLifecycleProvider: AppLifecycleProvider
    val photoShareMigrationManager: PhotoShareMigrationManager
}
