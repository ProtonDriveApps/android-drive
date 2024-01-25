/*
 * Copyright (c) 2021-2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.core.drive.drivelink.upload.domain.usecase

import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.drivelink.upload.domain.entity.Notifications
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.usecase.CreateUploadBulk
import me.proton.core.drive.upload.domain.exception.NotEnoughSpaceException
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import me.proton.core.drive.upload.domain.provider.FileProvider
import me.proton.core.drive.upload.domain.usecase.HasEnoughAvailableSpace
import javax.inject.Inject

class UploadFiles @Inject constructor(
    private val hasEnoughAvailableSpace: HasEnoughAvailableSpace,
    private val uploadWorkManager: UploadWorkManager,
    private val createUploadBulk: CreateUploadBulk,
    private val configurationProvider: ConfigurationProvider,
    private val validateUploadLimit: ValidateUploadLimit,
    private val fileProvider: FileProvider,
) {
    suspend operator fun invoke(
        folder: DriveLink.Folder,
        uploadFileDescriptions: List<UploadFileDescription>,
        notifications: Notifications = Notifications.TurnedOn,
        cacheOption: CacheOption = CacheOption.ALL,
        shouldDeleteSource: Boolean = false,
        background: Boolean = false,
        networkTypeProviderType: NetworkTypeProviderType = NetworkTypeProviderType.DEFAULT,
        shouldBroadcastErrorMessage: Boolean = true,
        priority: Long,
        tags : List<String> = emptyList(),
    ): Result<Unit> = coRunCatching {
        if (uploadFileDescriptions.isEmpty()) return@coRunCatching

        validateUploadLimit(folder.userId, uploadFileDescriptions.size)
            .onFailure {
                if (shouldDeleteSource) {
                    uploadFileDescriptions.forEach { uriString ->
                        fileProvider.getFile(uriString.uri).delete()
                    }
                }
            }
            .getOrThrow()
        if (background || uploadFileDescriptions.size > configurationProvider.bulkUploadThreshold) {
            processInBackground(
                folder = folder,
                uploadFileDescriptions = uploadFileDescriptions,
                notifications = notifications,
                cacheOption = cacheOption,
                networkTypeProviderType = networkTypeProviderType,
                priority = priority,
                shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
                shouldDeleteSource = shouldDeleteSource,
                tags = tags,
            )
        } else {
            processInForeground(
                folder = folder,
                uriStrings = uploadFileDescriptions.map { description -> description.uri },
                notifications = notifications,
                cacheOption = cacheOption,
                networkTypeProviderType = networkTypeProviderType,
                priority = priority,
                shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
                shouldDeleteSource = shouldDeleteSource,
            )
        }
    }

    private suspend fun processInForeground(
        folder: DriveLink.Folder,
        uriStrings: List<String>,
        notifications: Notifications,
        cacheOption: CacheOption,
        networkTypeProviderType: NetworkTypeProviderType,
        priority: Long,
        shouldBroadcastErrorMessage: Boolean,
        shouldDeleteSource: Boolean = false,
    ) =
        if (!hasEnoughAvailableSpace(folder.userId, uriStrings)) {
            if (shouldDeleteSource) {
                uriStrings.forEach { uriString ->
                    if (shouldDeleteSource) fileProvider.getFile(uriString).delete()
                }
            }
            throw NotEnoughSpaceException()
        } else {
            with(uploadWorkManager) {
                upload(
                    userId = folder.userId,
                    volumeId = folder.volumeId,
                    folderId = folder.id,
                    uriStrings = uriStrings,
                    shouldDeleteSource = shouldDeleteSource,
                    networkTypeProviderType = networkTypeProviderType,
                    shouldAnnounceEvent = notifications.system.announceUpload,
                    cacheOption = cacheOption,
                    priority = priority,
                    shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
                )
                if (notifications.inApp.showFilesBeingUploaded) {
                    broadcastFilesBeingUploaded(
                        folder = folder,
                        uriStrings = uriStrings,
                    )
                }
            }
        }

    private suspend fun processInBackground(
        folder: DriveLink.Folder,
        uploadFileDescriptions: List<UploadFileDescription>,
        notifications: Notifications,
        cacheOption: CacheOption,
        networkTypeProviderType: NetworkTypeProviderType,
        priority: Long,
        shouldDeleteSource: Boolean = false,
        shouldBroadcastErrorMessage: Boolean,
        tags : List<String> = emptyList(),
    ) =
        uploadWorkManager.upload(
            createUploadBulk(
                volumeId = folder.volumeId,
                parent = folder,
                uploadFileDescriptions = uploadFileDescriptions,
                shouldDeleteSource = shouldDeleteSource,
                networkTypeProviderType = networkTypeProviderType,
                shouldAnnounceEvent = notifications.system.announceUpload,
                cacheOption = cacheOption,
                priority = priority,
                shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
            ).getOrThrow(),
            folder,
            showPreparingUpload = notifications.inApp.showPreparingUpload,
            showFilesBeingUploaded = notifications.inApp.showFilesBeingUploaded,
            tags = tags,
        )
}
