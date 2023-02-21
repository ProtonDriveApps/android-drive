/*
 * Copyright (c) 2022-2023 Proton AG.
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

import me.proton.core.drive.base.domain.usecase.GetMediaResolution
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.UpdateLastModified
import me.proton.core.drive.linkupload.domain.usecase.UpdateMediaResolution
import me.proton.core.drive.linkupload.domain.usecase.UpdateSize
import me.proton.core.drive.linkupload.domain.usecase.UpdateUriString
import javax.inject.Inject

class UpdateUploadFileInfo @Inject constructor(
    private val updateUriString: UpdateUriString,
    private val updateSize: UpdateSize,
    private val updateLastModified: UpdateLastModified,
    private val updateMediaResolution: UpdateMediaResolution,
    private val getUploadFileSize: GetUploadFileSize,
    private val getUploadFileLastModified: GetUploadFileLastModified,
    private val getMediaResolution: GetMediaResolution,
) {
    suspend operator fun invoke(
       uploadFileLinkId: Long,
       uriString: String,
       shouldDeleteSourceUri: Boolean,
    ): Result<UploadFileLink> = coRunCatching {
        updateUriString(uploadFileLinkId, uriString, shouldDeleteSourceUri).getOrThrow().also { uploadFileLink ->
            getMediaResolution(uriString, uploadFileLink.mimeType)?.let { mediaResolution ->
                updateMediaResolution(uploadFileLinkId, mediaResolution).getOrThrow()
            }
        }
        updateSize(uploadFileLinkId, getUploadFileSize(uriString)).getOrThrow()
        updateLastModified(uploadFileLinkId, getUploadFileLastModified(uriString)).getOrThrow()
    }
}
