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

package me.proton.core.drive.thumbnail.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.link.domain.entity.RevisionContext
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.drive.sdk.entity.RevisionUid
import java.io.InputStream
import javax.inject.Inject

class GetThumbnailCachedInputStream @Inject constructor(
    private val getThumbnailSdk: GetThumbnailSdk,
    private val getThumbnailDecryptedFile: GetThumbnailDecryptedFile,
) {

    suspend operator fun invoke(
        userId: UserId,
        revisionUid: RevisionUid,
        volumeType: Volume.Type,
        type: ThumbnailType,
        inCacheFolder: Boolean,
    ): Result<InputStream> = coRunCatching {
        val thumbnailFile = getThumbnailDecryptedFile(
            userId = userId,
            revisionUid = revisionUid,
            type = type,
            inCacheFolder = inCacheFolder,
        )
        if (thumbnailFile.exists() && thumbnailFile.length() > 0) {
            thumbnailFile.inputStream()
        } else {
            thumbnailFile.createNewFile()
            thumbnailFile.outputStream().use { outputStream ->
                getThumbnailSdk(
                    revisionContext = RevisionContext(
                        userId = userId,
                        revisionUid = revisionUid,
                        volumeType = volumeType,
                    ),
                    thumbnailType = type,
                ).getOrThrow().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            thumbnailFile.inputStream()
        }
    }
}
