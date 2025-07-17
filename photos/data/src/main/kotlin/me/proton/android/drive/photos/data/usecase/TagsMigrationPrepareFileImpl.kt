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

package me.proton.android.drive.photos.data.usecase

import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag.PHOTO
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.GetDownloadState
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.DOWNLOADED
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.IDLE
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.PREPARED
import me.proton.core.drive.photo.domain.manager.PhotoTagWorkManager
import me.proton.core.drive.photo.domain.usecase.TagsMigrationPrepareFile
import me.proton.core.drive.photo.domain.usecase.UpdateTagsMigrationFileState
import me.proton.core.drive.photo.domain.usecase.UpdateTagsMigrationFileUri
import me.proton.core.drive.photo.domain.usecase.UpdateTagsMigrationMimeType
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class TagsMigrationPrepareFileImpl @Inject constructor(
    private val updateTagsMigrationFileState: UpdateTagsMigrationFileState,
    private val findLocalFile: FindLocalFile,
    private val getLink: GetLink,
    private val updateMimeType: UpdateTagsMigrationMimeType,
    private val updateUri: UpdateTagsMigrationFileUri,
    private val getDownloadState: GetDownloadState,
    private val photoTagWorkManager: PhotoTagWorkManager,
) : TagsMigrationPrepareFile {
    override suspend operator fun invoke(
        volumeId: VolumeId,
        fileId: FileId,
        startTagging: Boolean
    ): Result<String?> = coRunCatching {
        val mimeType = getLink(fileId).toResult().getOrNull()?.mimeType
        updateMimeType(volumeId, fileId, mimeType).getOrThrow()
        val typeCategory = mimeType?.toFileTypeCategory()

        if (typeCategory != FileTypeCategory.Image) {
            CoreLogger.d(
                PHOTO,
                "Skipping download for file: ${fileId.id.logId()} with type $typeCategory"
            )
            skipDownload(volumeId, fileId, startTagging)
            return@coRunCatching null
        }

        val uriString = findLocalFile(volumeId, fileId).getOrThrow()
        updateUri(volumeId, fileId, uriString).getOrThrow()

        if (uriString != null) {
            CoreLogger.d(
                PHOTO,
                "Uri found for photo: ${fileId.id.logId()}, skipping download ($uriString)"
            )
            skipDownload(volumeId, fileId, startTagging)
            return@coRunCatching uriString
        }

        val downloadState = getDownloadState(fileId).toResult().getOrThrow()

        if (downloadState is DownloadState.Downloaded) {
            CoreLogger.d(
                PHOTO,
                "Skipping download for already download file: ${fileId.id.logId()}"
            )
            skipDownload(volumeId, fileId, startTagging)
        } else {
            CoreLogger.d(
                PHOTO,
                "Uri not found for photo: ${fileId.id.logId()}, starting download"
            )
            updateTagsMigrationFileState(volumeId, fileId, PREPARED).getOrThrow()
        }
        null
    }.recoverCatching { error ->
        updateTagsMigrationFileState(volumeId, fileId, IDLE)
        throw error
    }

    private suspend fun TagsMigrationPrepareFileImpl.skipDownload(
        volumeId: VolumeId,
        fileId: FileId,
        startTagging: Boolean,
    ) {
        updateTagsMigrationFileState(volumeId, fileId, DOWNLOADED).getOrThrow()
        if (startTagging) {
            photoTagWorkManager.tag(volumeId, fileId)
        }
    }
}
