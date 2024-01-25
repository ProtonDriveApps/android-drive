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

package me.proton.android.drive.photos.presentation.viewmodel

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.data.extension.getDefaultMessage
import me.proton.android.drive.photos.domain.entity.PhotoBackupState
import me.proton.android.drive.photos.domain.usecase.TogglePhotosBackup
import me.proton.android.drive.photos.presentation.viewevent.BackupPermissionsViewEvent
import me.proton.android.drive.photos.presentation.viewstate.BackupPermissionsEffect
import me.proton.android.drive.photos.presentation.viewstate.BackupPermissionsViewState
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@Suppress("StaticFieldLeak")
class BackupPermissionsViewModelImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val backupPermissionsManager: BackupPermissionsManager,
    private val togglePhotosBackup: TogglePhotosBackup,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
    coroutineContext: CoroutineContext,
) : BackupPermissionsViewModel {
    private val coroutineScope = CoroutineScope(coroutineContext)
    private var folderId: FolderId? = null
    private var success: (suspend (PhotoBackupState) -> Unit)? = null
    private var navigateToPhotosPermissionRationale: (() -> Unit)? = null
    private val _backupPermissionsEffect = MutableSharedFlow<BackupPermissionsEffect>()
    private val backupPermissionsEffect: Flow<BackupPermissionsEffect> = _backupPermissionsEffect.asSharedFlow()
    override val initialViewState = BackupPermissionsViewState(
        permissions = backupPermissionsManager.requiredBackupPermissions,
        effect = backupPermissionsEffect,
    )

    override fun viewEvent(
        navigateToPhotosPermissionRationale: () -> Unit,
    ) = object : BackupPermissionsViewEvent {
        override val onPermissionsChanged = backupPermissionsManager::onPermissionChanged

        override val onPermissionResult = { result: Map<String, Boolean> ->
            val allPermissionsGranted = result.values.all { it }
            if (allPermissionsGranted) {
                toggleBackup()
            } else {
                navigateToPhotosPermissionRationale()
            }
        }
    }.also {
        this.navigateToPhotosPermissionRationale = navigateToPhotosPermissionRationale
    }

    override fun toggleBackup(folderId: FolderId, onSuccess: suspend (PhotoBackupState) -> Unit) {
        success = onSuccess
        this.folderId = folderId
        when (val backupPermissions = backupPermissionsManager.getBackupPermissions()) {
            BackupPermissions.Granted -> toggleBackup()
            is BackupPermissions.Denied -> if (backupPermissions.shouldShowRationale) {
                navigateToPhotosPermissionRationale?.invoke()
            } else {
                requestPermissions()
            }
        }
    }

    private fun requestPermissions() {
        coroutineScope.launch {
            _backupPermissionsEffect.emit(BackupPermissionsEffect.RequestPermission)
        }
    }

    private fun toggleBackup() {
        val folderId = requireNotNull(this.folderId)
        coroutineScope.launch {
            togglePhotosBackup(folderId)
                .onFailure { error ->
                    broadcastMessages(
                        userId = folderId.userId,
                        message = error.getDefaultMessage(appContext, configurationProvider.useExceptionMessage),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
                .onSuccess { state ->
                    success?.invoke(state)
                }
        }
    }
}
