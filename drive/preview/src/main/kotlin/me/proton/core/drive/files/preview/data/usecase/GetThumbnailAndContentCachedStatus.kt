/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.files.preview.data.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailFile
import java.io.File
import javax.inject.Inject

class GetThumbnailAndContentCachedStatus @Inject constructor(
    private val getDriveLink: GetDriveLink,
    private val getThumbnailFile: GetThumbnailFile,
    private val getPermanentFolder: GetPermanentFolder,
    private val getCacheFolder: GetCacheFolder,
) {

    suspend operator fun invoke(fileId: FileId): Result<Pair<Boolean, Boolean>> = coRunCatching {
        val (volumeId, revisionId) = getDriveLink(fileId)
            .toResult()
            .getOrThrow().let { driveLink -> driveLink.volumeId to driveLink.activeRevisionId }
        val thumbnailFile = getThumbnailFile(fileId.userId, volumeId, revisionId, ThumbnailType.PHOTO)
        val wasThumbnailCached = thumbnailFile != null && thumbnailFile.exists()
        val permanentFolder = getPermanentFolder(fileId.userId, volumeId.id, revisionId)
        val cacheFolder = getCacheFolder(fileId.userId, volumeId.id, revisionId)
        val wasContentCached = File(permanentFolder, FIRST_BLOCK_FILE_NAME).exists() ||
                File(cacheFolder, FIRST_BLOCK_FILE_NAME).exists()
        wasThumbnailCached to wasContentCached
    }

    companion object {
        private const val FIRST_BLOCK_FILE_NAME = "1"
    }
}
