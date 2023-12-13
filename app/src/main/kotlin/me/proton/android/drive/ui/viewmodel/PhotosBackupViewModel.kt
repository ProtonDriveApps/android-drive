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

package me.proton.android.drive.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.android.drive.photos.domain.entity.PhotoBackupState
import me.proton.android.drive.photos.presentation.viewmodel.BackupPermissionsViewModel
import me.proton.android.drive.ui.viewevent.PhotosBackupViewEvent
import me.proton.android.drive.ui.viewstate.PhotosBackupViewState
import me.proton.core.drive.backup.domain.manager.BackupManager
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@Suppress("StaticFieldLeak")
@HiltViewModel
class PhotosBackupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    backupManager: BackupManager,
    private val broadcastMessages: BroadcastMessages,
    val backupPermissionsViewModel: BackupPermissionsViewModel,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val initialViewState: PhotosBackupViewState = PhotosBackupViewState(
        title = appContext.getString(I18N.string.photos_backup_title),
        enableBackupTitle = appContext.getString(I18N.string.photos_backup_title),
        isBackupEnabled = false,
    )
    val viewState: Flow<PhotosBackupViewState> = backupManager.isEnabled(userId).map { enabled ->
        initialViewState.copy(
            isBackupEnabled = enabled,
        )
    }

    fun viewEvent(
        navigateBack: () -> Unit,
    ): PhotosBackupViewEvent = object : PhotosBackupViewEvent {
        override val onToggle = {
            backupPermissionsViewModel.toggleBackup(userId) { state ->
                with(state) {
                    broadcastMessages(
                        userId = userId,
                        message = message,
                        type = type,
                    )
                    action()
                }
            }
        }

        private val PhotoBackupState.message
            get() = when (this) {
                PhotoBackupState.Disabled -> appContext.getString(
                    I18N.string.photos_backup_in_app_notification_turned_off
                )

                is PhotoBackupState.Enabled -> appContext.getString(
                    I18N.string.photos_backup_in_app_notification_turned_on
                )

                is PhotoBackupState.NoFolder -> appContext.getString(
                    I18N.string.photos_error_no_folders
                ).format(folderName)
            }

        private val PhotoBackupState.type
            get() = when (this) {
                PhotoBackupState.Disabled -> BroadcastMessage.Type.INFO
                is PhotoBackupState.Enabled -> BroadcastMessage.Type.INFO
                is PhotoBackupState.NoFolder -> BroadcastMessage.Type.WARNING
            }

        private val PhotoBackupState.action: () -> Unit
            get() = when (this) {
                PhotoBackupState.Disabled -> navigateBack
                is PhotoBackupState.Enabled -> navigateBack
                is PhotoBackupState.NoFolder -> { -> }
            }
    }
}
