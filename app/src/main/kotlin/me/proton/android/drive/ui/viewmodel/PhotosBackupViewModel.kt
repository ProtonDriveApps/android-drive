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

package me.proton.android.drive.ui.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.entity.PhotoBackupState
import me.proton.android.drive.photos.domain.usecase.GetPhotosConfiguration
import me.proton.android.drive.photos.domain.usecase.GetPhotosDriveLink
import me.proton.android.drive.photos.domain.usecase.IsPhotosEnabled
import me.proton.android.drive.photos.domain.usecase.TogglePhotosNetworkConfiguration
import me.proton.android.drive.photos.presentation.viewmodel.BackupPermissionsViewModel
import me.proton.android.drive.ui.viewevent.PhotosBackupViewEvent
import me.proton.android.drive.ui.viewstate.PhotosBackupOption
import me.proton.android.drive.ui.viewstate.PhotosBackupViewState
import me.proton.core.drive.backup.domain.entity.BackupNetworkType
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.firstSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.usecase.IsIgnoringBatteryOptimizations
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.extension.launchIgnoreBatteryOptimizations
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@Suppress("StaticFieldLeak")
@HiltViewModel
class PhotosBackupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    isPhotosEnabled: IsPhotosEnabled,
    getPhotosConfiguration: GetPhotosConfiguration,
    private val togglePhotosNetworkConfiguration: TogglePhotosNetworkConfiguration,
    private val getPhotosDriveLink: GetPhotosDriveLink,
    private val broadcastMessages: BroadcastMessages,
    private val isIgnoringBatteryOptimizations: IsIgnoringBatteryOptimizations,
    private val configurationProvider: ConfigurationProvider,
    val backupPermissionsViewModel: BackupPermissionsViewModel,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    val initialViewState: PhotosBackupViewState = PhotosBackupViewState(
        title = appContext.getString(I18N.string.photos_backup_title),
        backup = PhotosBackupOption(
            title = appContext.getString(I18N.string.photos_backup_title),
            checked = false,
        ),
        mobileData = PhotosBackupOption(
            title = appContext.getString(I18N.string.photos_backup_option_mobile_data),
            checked = false,
            enabled = false,
        ),
        ignoringBatteryOptimizations = PhotosBackupOption(
            title = appContext.getString(I18N.string.photos_backup_option_ignoring_battery_optimizations),
            description = appContext.getString(I18N.string.photos_backup_option_ignoring_battery_optimizations_description),
            checked = false,
            enabled = false,
        )
    )
    val viewState: Flow<PhotosBackupViewState> = combine(
        isPhotosEnabled(userId),
        getPhotosConfiguration(userId),
        isIgnoringBatteryOptimizations(),
    ) { enabled, configuration, isIgnoringBatteryOptimizations ->
        initialViewState.copy(
            backup = initialViewState.backup.copy(checked = enabled),
            mobileData = initialViewState.mobileData.copy(
                checked = configuration?.networkType == BackupNetworkType.CONNECTED,
                enabled = enabled,
            ),
            ignoringBatteryOptimizations = initialViewState.ignoringBatteryOptimizations.copy(
                checked = isIgnoringBatteryOptimizations,
                enabled = enabled
            )
        )
    }

    fun viewEvent(
        navigateBack: () -> Unit,
    ): PhotosBackupViewEvent = object : PhotosBackupViewEvent {
        override val onToggleBackup = {
            viewModelScope.launch {
                coRunCatching {
                    val photoRootId = getPhotosDriveLink(userId)
                        .firstSuccessOrError().toResult().getOrThrow().id
                    backupPermissionsViewModel.toggleBackup(photoRootId) { state ->
                        with(state) {
                            broadcastMessages(
                                userId = userId,
                                message = message,
                                type = type,
                            )
                            action()
                        }
                    }
                }.onFailure { error ->
                    error.log(BACKUP)
                    broadcastMessages(
                        userId = userId,
                        message = error.getDefaultMessage(
                            appContext,
                            configurationProvider.useExceptionMessage
                        ),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
            }
            Unit
        }
        override val onToggleMobileData = {
            viewModelScope.launch {
                togglePhotosNetworkConfiguration(userId).onFailure { error ->
                    error.log(BACKUP)
                    broadcastMessages(
                        userId = userId,
                        message = error.getDefaultMessage(
                            appContext,
                            configurationProvider.useExceptionMessage
                        ),
                        type = BroadcastMessage.Type.ERROR,
                    )
                }
            }
            Unit
        }

        override val onToggleIgnoringBatteryOptimizations: (Context) -> Unit = { context ->
            viewModelScope.launch {
                context.launchIgnoreBatteryOptimizations(isIgnoringBatteryOptimizations().firstOrNull() == true)
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
