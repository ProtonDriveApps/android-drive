/*
 * Copyright (c) 2023-2024 Proton AG.
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.android.drive.extension.log
import me.proton.android.drive.photos.domain.usecase.RescanOnMediaStoreUpdate
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountRemoved
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.handler.UploadErrorHandler
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.usecase.CheckAvailableSpace
import me.proton.core.drive.backup.domain.usecase.HasFolders
import me.proton.core.drive.backup.domain.usecase.ObserveConfigurationChanges
import me.proton.core.drive.backup.domain.usecase.StartBackupAfterErrorResolved
import me.proton.core.drive.backup.domain.usecase.UnwatchFolders
import me.proton.core.drive.backup.domain.usecase.WatchFolders
import me.proton.core.drive.base.domain.extension.mapWithPrevious
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.upload.domain.manager.UploadErrorManager
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.user.domain.UserManager
import me.proton.core.util.kotlin.CoreLogger

class BackupInitializer : Initializer<Unit> {

    private val scopes = mutableMapOf<UserId, CoroutineScope>()

    override fun create(context: Context) {

        EntryPointAccessors.fromApplication(
            context.applicationContext,
            BackupInitializerEntryPoint::class.java
        ).run {
            if (!configurationProvider.photosFeatureFlag) {
                CoreLogger.d(BACKUP, "Backup feature disabled")
                return
            }
            uploadErrorManager.errors
                .onEach(uploadErrorHandler::onError)
                .launchIn(appLifecycleProvider.lifecycle.coroutineScope)
            accountManager.observe(appLifecycleProvider.lifecycle, Lifecycle.State.STARTED)
                .onAccountReady { account ->
                    val userId = account.userId
                    val scope = scopes.getOrPut(userId) {
                        CoroutineScope(Dispatchers.IO + Job())
                    }

                    backupPermissionsManager.backupPermissions.mapWithPrevious { previous, permissions ->
                        previous is BackupPermissions.Denied && permissions == BackupPermissions.Granted
                    }.filter { acquirePermissions ->
                        acquirePermissions
                    }.onEach {
                        startBackupAfterErrorResolved(
                            userId = userId,
                            type = BackupErrorType.PERMISSION,
                        ).onFailure { error ->
                            error.log(BACKUP, "Cannot restart the backup")
                        }
                    }.launchIn(scope)

                    hasFolders(userId).onEach { hasFolders ->
                        if (hasFolders) {
                            watchFolders(userId).onFailure { error ->
                                error.log(BACKUP, "Cannot watch folders")
                            }
                        } else {
                            unwatchFolders(userId).onFailure { error ->
                                error.log(BACKUP, "Cannot unwatch folders")
                            }
                        }
                    }.launchIn(scope)

                    observeConfigurationChanges(userId).launchIn(scope)

                    userManager.observeUser(userId).filterNotNull().onEach { user ->
                        checkAvailableSpace(user).onFailure { error ->
                            error.log(BACKUP, "Cannot check available space")
                        }
                    }.launchIn(scope)

                    rescanOnMediaStoreUpdate(userId).onFailure { error ->
                        error.log("Cannot observe media store updates")
                    }
                }
                .onAccountRemoved { account ->
                    unwatchFolders(account.userId)
                    scopes.remove(account.userId)?.cancel()
                }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        WorkManagerInitializer::class.java,
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupInitializerEntryPoint {
        val accountManager: AccountManager
        val checkAvailableSpace: CheckAvailableSpace
        val hasFolders: HasFolders
        val userManager: UserManager
        val watchFolders: WatchFolders
        val unwatchFolders: UnwatchFolders
        val uploadErrorHandler: UploadErrorHandler
        val uploadErrorManager: UploadErrorManager
        val backupPermissionsManager: BackupPermissionsManager
        val startBackupAfterErrorResolved: StartBackupAfterErrorResolved
        val appLifecycleProvider: AppLifecycleProvider
        val configurationProvider: ConfigurationProvider
        val rescanOnMediaStoreUpdate: RescanOnMediaStoreUpdate
        val observeConfigurationChanges: ObserveConfigurationChanges
    }
}
