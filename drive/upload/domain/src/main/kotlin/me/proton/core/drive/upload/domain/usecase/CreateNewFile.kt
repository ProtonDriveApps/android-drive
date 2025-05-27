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
package me.proton.core.drive.upload.domain.usecase

import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.create.domain.usecase.CreateNewFile
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.UpdateLinkFileInfo
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadFileCreationTime
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.upload.domain.resolver.UriResolver
import java.io.FileNotFoundException
import javax.inject.Inject

class CreateNewFile @Inject constructor(
    private val isUploadFileExist: IsUploadFileExist,
    private val createNewFile: CreateNewFile,
    private val getLink: GetLink,
    private val updateUploadState: UpdateUploadState,
    private val updateUploadFileCreationTime: UpdateUploadFileCreationTime,
    private val updateLinkFileInfo: UpdateLinkFileInfo,
    private val uriResolver: UriResolver,
) {
    suspend operator fun invoke(uploadFileLink: UploadFileLink): Result<UploadFileLink> = coRunCatching {
        with (uploadFileLink) {
            uriString?.let { uriString ->
                checkFileExists(uriString)
            }
            updateUploadState(id, UploadState.CREATING_NEW_FILE).getOrThrow()
            updateUploadFileCreationTime(id, TimestampS()).getOrThrow()
            try {
                val (newFileInfo, fileInfo) = createNewFile(
                    parentFolder = getLink(parentLinkId).toResult().getOrThrow(),
                    name = name,
                    mimeType = mimeType,
                ).getOrThrow()
                updateLinkFileInfo(
                    uploadFileLinkId = id,
                    newFileInfo = newFileInfo,
                    fileInfo = fileInfo,
                ).getOrThrow()
            } catch (e: Throwable) {
                updateUploadState(id, UploadState.IDLE)
                throw e
            }
        }
    }

    private suspend fun checkFileExists(uriString: String) {
        val uploadFileExist = isUploadFileExist(uriString)
        if (!uploadFileExist) {
            throw FileNotFoundException("File does not exist or is trashed $uriString")
        }
        uriResolver.useInputStream(uriString) {}
    }
}
