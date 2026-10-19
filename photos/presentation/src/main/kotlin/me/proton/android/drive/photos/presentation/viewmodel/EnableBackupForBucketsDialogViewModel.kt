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

package me.proton.android.drive.photos.presentation.viewmodel

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.android.drive.photos.domain.entity.PhotoBackupState
import me.proton.android.drive.photos.domain.usecase.EnablePhotosBackup
import me.proton.android.drive.photos.domain.usecase.GetPhotosDriveLink
import me.proton.android.drive.photos.presentation.viewevent.EnableBackupForBucketsViewEvent
import me.proton.android.drive.photos.presentation.viewstate.EnableBackupForBucketsViewState
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.usecase.GetAllBuckets
import me.proton.core.drive.base.data.extension.getDefaultMessage
import me.proton.core.drive.base.data.extension.log
import me.proton.core.drive.base.domain.coroutines.ApplicationScope
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.BACKUP
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.BroadcastMessages
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.presentation.component.RunAction
import me.proton.core.drive.base.presentation.extension.quantityString
import me.proton.core.drive.base.presentation.viewmodel.UserViewModel
import me.proton.core.drive.messagequeue.domain.entity.BroadcastMessage
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import me.proton.core.drive.i18n.R as I18N

@HiltViewModel
class EnableBackupForBucketsDialogViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    getAllBuckets: GetAllBuckets,
    getPhotosDriveLink: GetPhotosDriveLink,
    backupPermissionsManager: BackupPermissionsManager,
    private val enablePhotosBackup: EnablePhotosBackup,
    private val broadcastMessages: BroadcastMessages,
    private val configurationProvider: ConfigurationProvider,
) : ViewModel(), UserViewModel by UserViewModel(savedStateHandle) {

    private val selectedBucketIds = MutableStateFlow<Set<Int>>(emptySet())

    private val photosDriveLink = getPhotosDriveLink(userId).filterSuccessOrError()

    val initialViewState = EnableBackupForBucketsViewState(
        title = appContext.getString(I18N.string.photos_folder_selection_title),
        description = appContext.getString(I18N.string.photos_folder_selection_description),
    )

    val viewState: Flow<EnableBackupForBucketsViewState> = combine(
        backupPermissionsManager.backupPermissions,
        getAllBuckets().filterNotNull(),
        selectedBucketIds,
    ) { permissions, buckets, selectedIds ->
        val bucketItems = if (permissions is BackupPermissions.Granted) {
            buckets.map { entry ->
                EnableBackupForBucketsViewState.BucketItem(
                    id = entry.bucketId,
                    name = entry.bucketName ?: entry.bucketId.toString(),
                    description = buildBucketDescription(
                        imageCount = entry.imageCount,
                        videoCount = entry.videoCount,
                    ),
                    uri = entry.lastItemUriString?.toUri(),
                    isSelected = selectedIds.contains(entry.bucketId),
                )
            }
        } else {
            emptyList()
        }
        initialViewState.copy(
            buckets = bucketItems,
            hasSelection = selectedIds.isNotEmpty(),
        )
    }

    fun viewEvent(
        runAction: RunAction,
    ): EnableBackupForBucketsViewEvent = object : EnableBackupForBucketsViewEvent {
        override val onToggleBucket: (Int, Boolean) -> Unit = { bucketId, isSelected ->
            selectedBucketIds.update { currentIds ->
                if (isSelected) {
                    currentIds + bucketId
                } else {
                    currentIds - bucketId
                }
            }
        }

        override val onSave: () -> Unit = {
            runAction {
                save()
            }
        }

        override val onDismiss: () -> Unit = {
            runAction { }
        }
    }

    private fun save() {
        applicationScope.launch {
            coRunCatching {
                val selectedIds = selectedBucketIds.value
                val folderId = photosDriveLink.toResult().getOrThrow().id
                enablePhotosBackup(folderId) { bucket ->
                    bucket.bucketId in selectedIds
                }.getOrThrow()
            }.onSuccess { backupState ->
                when (backupState) {
                    is PhotoBackupState.Enabled -> {
                        val folderNames = backupState.folderNames
                        val folderCount = folderNames.size
                        broadcastMessages(
                            userId = userId,
                            message = appContext.resources.getQuantityString(
                                I18N.plurals.photos_message_folders_setup,
                                folderCount
                            )
                                .format(folderNames.joinToString(", "), folderCount),
                            type = BroadcastMessage.Type.INFO,
                        )
                    }

                    else -> CoreLogger.w(BACKUP, "Failed to enable backup $backupState")
                }
            }.onFailure { error ->
                error.log(BACKUP, "Failed to setup photos backup")
                broadcastMessages(
                    userId = userId,
                    message = error.getDefaultMessage(
                        context = appContext,
                        useExceptionMessage = configurationProvider.useExceptionMessage,
                    ),
                    type = BroadcastMessage.Type.ERROR,
                )
            }
        }
    }

    private fun buildBucketDescription(imageCount: Int, videoCount: Int): String {
        return listOfNotNull(
            imageCount.takeIf { count -> count > 0 }?.let { count ->
                appContext.quantityString(
                    I18N.plurals.settings_photos_backup_folders_description_photos,
                    count,
                ).format(count)
            },
            videoCount.takeIf { count -> count > 0 }?.let { count ->
                appContext.quantityString(
                    I18N.plurals.settings_photos_backup_folders_description_videos,
                    count,
                ).format(count)
            },
        ).joinToString(", ")
    }
}
