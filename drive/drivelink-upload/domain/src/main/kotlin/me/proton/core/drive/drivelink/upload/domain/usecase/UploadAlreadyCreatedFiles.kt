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

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.upload.domain.exception.NotEnoughSpaceException
import me.proton.core.drive.upload.domain.manager.UploadWorkManager
import me.proton.core.drive.upload.domain.usecase.HasEnoughAvailableSpace
import javax.inject.Inject

class UploadAlreadyCreatedFiles @Inject constructor(
    private val uploadWorkManager: UploadWorkManager,
    private val hasEnoughAvailableSpace: HasEnoughAvailableSpace,
    private val validateUploadLimit: ValidateUploadLimit,
) {
    suspend operator fun invoke(
        folder: DriveLink.Folder,
        uploadFiles: List<Pair<UploadFileLink, String>>,
        shouldDeleteSource: Boolean = false,
        showFilesBeingUploaded: Boolean = true,
    ): Result<Unit> = coRunCatching {
        validateUploadLimit(folder.userId, uploadFiles.size).getOrThrow()
        with (uploadWorkManager) {
            val uriStrings = uploadFiles.map { (_, uriString) -> uriString }
            if (!hasEnoughAvailableSpace(folder.userId, uriStrings)) {
                uploadFiles.map { (uploadFileLink, _) -> uploadFileLink }.forEach { uploadFileLink ->
                    cancel(uploadFileLink)
                }
                throw NotEnoughSpaceException()
            } else {
                uploadFiles.forEach { (uploadFileLink, uriString) ->
                    uploadAlreadyCreated(
                        userId = folder.userId,
                        uploadFileLinkId = uploadFileLink.id,
                        uriString = uriString,
                        shouldDeleteSource = shouldDeleteSource,
                    )
                }
                if (showFilesBeingUploaded) {
                    broadcastFilesBeingUploaded(
                        folder = folder,
                        uriStrings = uriStrings,
                    )
                }
            }
        }
    }
}
