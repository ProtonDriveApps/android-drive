/*
 * Copyright (c) 2021-2023 Proton AG.
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
import me.proton.core.drive.link.domain.extension.userId
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
        uriStrings: List<String>,
        shouldDeleteSource: Boolean = false,
        silently: Boolean = false,
    ): Result<Unit> = coRunCatching {
        validateUploadLimit(folder.userId, uriStrings.size)
            .onFailure {
                if (shouldDeleteSource) {
                    uriStrings.forEach { uriString ->
                        fileProvider.getFile(uriString).delete()
                    }
                }
            }
            .getOrThrow()
        when {
            uriStrings.isEmpty() -> return@coRunCatching
            uriStrings.size <= configurationProvider.bulkUploadThreshold -> processInForeground(
                folder, uriStrings, shouldDeleteSource
            )
            else -> processInBackground(folder, uriStrings, shouldDeleteSource, silently)
        }
    }

    private suspend fun processInForeground(
        folder: DriveLink.Folder,
        uriStrings: List<String>,
        shouldDeleteSource: Boolean = false,
    )=
        if (!hasEnoughAvailableSpace(folder.userId, uriStrings)) {
            if (shouldDeleteSource) {
                uriStrings.forEach { uriString ->
                    if (shouldDeleteSource) fileProvider.getFile(uriString).delete()
                }
            }
            throw NotEnoughSpaceException()
        } else {
            with (uploadWorkManager) {
                upload(
                    userId = folder.userId,
                    volumeId = folder.volumeId,
                    folderId = folder.id,
                    uriStrings = uriStrings,
                    shouldDeleteSource = shouldDeleteSource,
                )
                broadcastFilesBeingUploaded(
                    folder = folder,
                    uriStrings = uriStrings,
                )
            }
        }

    private suspend fun processInBackground(
        folder: DriveLink.Folder,
        uriStrings: List<String>,
        shouldDeleteSource: Boolean = false,
        silently: Boolean = false,
    ) =
        uploadWorkManager.upload(
            createUploadBulk(
                volumeId = folder.volumeId,
                parent = folder,
                uriStrings = uriStrings,
                shouldDeleteSource = shouldDeleteSource,
            ).getOrThrow(),
            folder,
            silently = silently,
        )
}
