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

import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.coroutines.FileScope
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.file.base.domain.repository.FileRepository
import me.proton.core.drive.file.base.domain.usecase.DownloadUrl
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Link.Companion.THUMBNAIL_INDEX
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class GetThumbnailBlock @Inject constructor(
    private val getThumbnailCacheFile: GetThumbnailCacheFile,
    private val downloadUrl: DownloadUrl,
    private val fileRepository: FileRepository,
) {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend operator fun invoke(
        fileId: FileId,
        volumeId: VolumeId,
        revisionId: String,
        coroutineContext: CoroutineContext = FileScope.coroutineContext
    ): Result<Block> = coRunCatching(coroutineContext) {
        val thumbnail = getThumbnailCacheFile(fileId.userId, volumeId, revisionId)
            .takeIf { thumbnailFile -> thumbnailFile.exists() }
            ?: downloadUrl(
                userId = fileId.userId,
                url = fileRepository.fetchThumbnailUrl(fileId, revisionId).getOrThrow(),
                destination = getThumbnailCacheFile(fileId.userId, volumeId, revisionId).apply { createNewFile() },
                progress = MutableStateFlow(0L),
            ).getOrThrow()
        Block(
            index = THUMBNAIL_INDEX,
            url = thumbnail.path,
            hashSha256 = null,
            encSignature = null,
        )
    }
}
