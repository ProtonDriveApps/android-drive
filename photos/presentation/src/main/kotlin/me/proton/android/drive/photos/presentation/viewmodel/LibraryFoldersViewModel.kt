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

package me.proton.android.drive.photos.presentation.viewmodel

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.usecase.GetPhotosDriveLink
import me.proton.android.drive.photos.domain.usecase.SetupPhotosConfigurationBackup
import me.proton.android.drive.photos.presentation.state.LibraryFolder
import me.proton.android.drive.photos.presentation.state.LibraryFoldersState
import me.proton.android.drive.photos.presentation.viewevent.LibraryFoldersViewEvent
import me.proton.core.domain.arch.onSuccess
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.entity.BucketEntry
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.usecase.EnableBackupForFolder
import me.proton.core.drive.backup.domain.usecase.GetAllBuckets
import me.proton.core.drive.backup.domain.usecase.GetFoldersFlow
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.onFailure
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.extension.require
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@Suppress("StaticFieldLeak")
@HiltViewModel
class LibraryFoldersViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getAllBuckets: GetAllBuckets,
    getFoldersFlow: GetFoldersFlow,
    getPhotosDriveLink: GetPhotosDriveLink,
    private val configurationProvider: ConfigurationProvider,
    private val broadcastMessages: BroadcastMessages,
    private val enableBackupForFolder: EnableBackupForFolder,
    private val setupPhotosConfigurationBackup: SetupPhotosConfigurationBackup,
    backupPermissionsManager: BackupPermissionsManager,
) : ViewModel() {

    private val userId = UserId(savedStateHandle.require("userId"))

    val driveLink: StateFlow<DriveLink.Folder?> = getPhotosDriveLink(userId)
        .filterSuccessOrError()
        .map { result ->
            result
                .onSuccess { driveLink ->
                    return@map driveLink
                }
                .onFailure { error ->
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
            return@map null
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val folders = driveLink.filterNotNull().flatMapLatest { folder ->
        getFoldersFlow(folder.id)
    }

    val state = combine(
        backupPermissionsManager.backupPermissions,
        getAllBuckets().filterNotNull(),
        folders,
    ) { permissions, entries, folders ->
        if (permissions is BackupPermissions.Granted) {
            val libraryFolders = merge(entries, folders)
                .sortedByDescending { entry -> entry.enabled }
            val defaultBucketName = configurationProvider.backupDefaultBucketName
            LibraryFoldersState.Content(
                title = appContext.getString(I18N.string.settings_photos_backup_folders_title),
                description = appContext.getString(I18N.string.settings_photos_backup_folders_description)
                    .format(defaultBucketName),
                folders = if (libraryFolders.none { folder -> folder.name == defaultBucketName }) {
                    listOf(
                        LibraryFolder.NotFound(
                            name = defaultBucketName,
                            description = appContext.getString(I18N.string.settings_photos_backup_folders_not_found)
                        )
                    ) + libraryFolders
                } else {
                    libraryFolders
                }
            )
        } else {
            LibraryFoldersState.NoPermissions
        }
    }

    private fun merge(
        entries: List<BucketEntry>,
        folders: List<BackupFolder>,
    ) = entries.associateWith { entry ->
        folders.firstOrNull { folder ->
            folder.bucketId == entry.bucketId
        }
    }.map { (entry, folder) ->
        LibraryFolder.Entry(
            id = entry.bucketId,
            name = entry.bucketName ?: entry.bucketId.toString(),
            description = listOfNotNull(
                entry.imageCount.takeIf { count -> count > 0 }?.let { count ->
                    appContext.quantityString(
                        I18N.plurals.settings_photos_backup_folders_description_photos,
                        count,
                    ).format(count)
                },
                entry.videoCount.takeIf { count -> count > 0 }?.let { count ->
                    appContext.quantityString(
                        I18N.plurals.settings_photos_backup_folders_description_videos,
                        count,
                    ).format(count)
                },
            ).joinToString(", "),
            uri = entry.lastItemUriString?.toUri(),
            enabled = folder != null,
        )
    }

    fun viewEvent(
        navigateToConfirmStopSyncFolder: (FolderId, Int) -> Unit,
    ) = object : LibraryFoldersViewEvent {
        override val onToggleBucket: (Int, Boolean) -> Unit = { id, enable ->
            viewModelScope.launch {
                coRunCatching {
                    val folder = requireNotNull(driveLink.value)
                    val backupFolder = BackupFolder(
                        bucketId = id,
                        folderId = folder.id
                    )
                    if (enable) {
                        setupPhotosConfigurationBackup(folder.id).getOrThrow()
                        enableBackupForFolder(backupFolder).getOrThrow()
                    } else {
                        navigateToConfirmStopSyncFolder(
                            backupFolder.folderId,
                            backupFolder.bucketId,
                        )
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
        }
    }
}
