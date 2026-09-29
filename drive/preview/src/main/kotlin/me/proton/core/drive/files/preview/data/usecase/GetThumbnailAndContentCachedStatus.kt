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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.usecase.GetCacheFolder
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.drivelink.domain.extension.revisionUid
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.decryptedFileName
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.thumbnail.domain.usecase.GetThumbnailDecryptedFile
import me.proton.drive.sdk.entity.RevisionUid
import java.io.File
import javax.inject.Inject

class GetThumbnailAndContentCachedStatus @Inject constructor(
    private val getDriveLink: GetDriveLink,
    private val getThumbnailDecryptedFile: GetThumbnailDecryptedFile,
    private val getPermanentFolder: GetPermanentFolder,
    private val getCacheFolder: GetCacheFolder,
) {

    suspend operator fun invoke(fileId: FileId): Result<Pair<Boolean, Boolean>> = coRunCatching {
        val driveLink = getDriveLink(fileId).toResult().getOrThrow()
        invoke(fileId.userId, driveLink.revisionUid).getOrThrow()
    }

    suspend operator fun invoke(
        userId: UserId,
        revisionUid: RevisionUid,
    ): Result<Pair<Boolean, Boolean>> = coRunCatching {
        val thumbnailFile = getThumbnailDecryptedFile(
            userId = userId,
            revisionUid = revisionUid,
            type = ThumbnailType.PHOTO,
        )
        val wasThumbnailCached = thumbnailFile != null && thumbnailFile.exists()
        val fileName = revisionUid.decryptedFileName
        val wasContentCached = File(getPermanentFolder(userId, revisionUid), fileName).exists() ||
                File(getCacheFolder(userId, revisionUid), fileName).exists()
        wasThumbnailCached to wasContentCached
    }
}
