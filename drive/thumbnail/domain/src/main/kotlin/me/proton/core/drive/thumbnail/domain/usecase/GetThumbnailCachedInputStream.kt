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

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptThumbnail
import me.proton.core.drive.file.base.domain.entity.ThumbnailId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId
import java.io.InputStream
import javax.inject.Inject

class GetThumbnailCachedInputStream @Inject constructor(
    private val getThumbnailInputStream: GetThumbnailInputStream,
    private val getThumbnailFile: GetThumbnailFile,
    private val getThumbnailDecryptedFile: GetThumbnailDecryptedFile,
    private val decryptThumbnail: DecryptThumbnail,
) {

    suspend operator fun invoke(
        fileId: FileId,
        volumeId: VolumeId,
        revisionId: String,
        thumbnailId: ThumbnailId,
        inCacheFolder: Boolean
    ): Result<InputStream> = coRunCatching {
        val encryptedThumbnailFile = getThumbnailFile(
            userId = fileId.userId,
            volumeId = volumeId,
            revisionId = revisionId,
            type = thumbnailId.type,
        )
        val decryptedThumbnailFile = getThumbnailDecryptedFile(
            userId = fileId.userId,
            volumeId = volumeId,
            revisionId = revisionId,
            type = thumbnailId.type,
            inCacheFolder = inCacheFolder,
        )
        if (decryptedThumbnailFile.exists() && decryptedThumbnailFile.length() > 0) {
            decryptedThumbnailFile.inputStream()
        } else if (encryptedThumbnailFile != null
            && encryptedThumbnailFile.exists()
            && encryptedThumbnailFile.length() > 0
        ) {
            decryptedThumbnailFile.outputStream().use { outputStream ->
                outputStream.write(
                    decryptThumbnail(fileId, encryptedThumbnailFile.inputStream()).getOrThrow()
                )
            }
            decryptedThumbnailFile.inputStream()
        } else {
            decryptedThumbnailFile.createNewFile()
            decryptedThumbnailFile.outputStream().use { outputStream ->
                getThumbnailInputStream(thumbnailId).getOrThrow().use { inputStream ->
                    outputStream.write(decryptThumbnail(fileId, inputStream).getOrThrow())
                }
            }
            decryptedThumbnailFile.inputStream()
        }
    }
}
