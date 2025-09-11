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
import me.proton.core.drive.file.base.domain.entity.ThumbnailId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId
import java.io.InputStream
import javax.inject.Inject

class GetThumbnailCachedInputStream @Inject constructor(
    private val getThumbnailInputStream: GetThumbnailInputStream,
    private val getThumbnailFile: GetThumbnailFile,
) {

    suspend operator fun invoke(
        fileId: FileId,
        volumeId: VolumeId,
        revisionId: String,
        thumbnailId: ThumbnailId,
        fetchFromNetworkIfDoesNotExist: Boolean = true,
    ): Result<InputStream> = coRunCatching {
        val thumbnailFile = getThumbnailFile(fileId.userId, volumeId, revisionId, thumbnailId.type)
        if ((thumbnailFile == null || !thumbnailFile.exists()) && !fetchFromNetworkIfDoesNotExist) {
            return Result.failure(
                IllegalStateException("${thumbnailFile?.path} doesn't exists and fetchFromNetworkIfDoesNotExist is false")
            )
        }
        val cacheFile = getThumbnailFile(fileId.userId, volumeId, revisionId, thumbnailId.type, true)
        if (!cacheFile.exists()) {
            cacheFile.createNewFile()
            cacheFile.outputStream().use { outputStream ->
                getThumbnailInputStream(thumbnailId)
                    .getOrThrow().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
            }
        }
        cacheFile.inputStream()
    }
}
