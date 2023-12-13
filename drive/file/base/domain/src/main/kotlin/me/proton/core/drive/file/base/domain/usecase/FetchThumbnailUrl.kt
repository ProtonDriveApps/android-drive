/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.file.base.domain.usecase

import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.domain.entity.ThumbnailId
import me.proton.core.drive.file.base.domain.entity.ThumbnailUrl
import me.proton.core.drive.file.base.domain.extension.getThumbnailId
import me.proton.core.drive.file.base.domain.repository.FileRepository
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject

class FetchThumbnailUrl @Inject constructor(
    private val fileRepository: FileRepository,
    private val getLink: GetLink,
) {
    suspend operator fun invoke(
        thumbnailId: ThumbnailId,
    ): Result<ThumbnailUrl> = coRunCatching {
        fileRepository.fetchThumbnailsUrls(
            userId = thumbnailId.userId,
            volumeId = thumbnailId.volumeId,
            thumbnailIds = setOf(
                when (thumbnailId) {
                    is ThumbnailId.Legacy -> refreshThumbnailId(thumbnailId)
                    is ThumbnailId.File -> thumbnailId
                }
            ),
        ).getValue(thumbnailId).getOrThrow()
    }

    suspend operator fun invoke(
        thumbnailIds: Set<ThumbnailId>,
    ): Map<ThumbnailId, Result<ThumbnailUrl>> =
        if (thumbnailIds.isNotEmpty()) {
            require(thumbnailIds.groupBy { it.userId }.keys.size == 1) {
                "All thumbnail ids must be from same user"
            }
            require(thumbnailIds.groupBy { it.volumeId }.keys.size == 1) {
                "All thumbnail ids must be from same volume"
            }
            fileRepository.fetchThumbnailsUrls(
                userId = thumbnailIds.first().userId,
                volumeId = thumbnailIds.first().volumeId,
                thumbnailIds = thumbnailIds.map { id ->
                    when (id) {
                        is ThumbnailId.Legacy -> refreshThumbnailId(id)
                        is ThumbnailId.File -> id
                    }
                }.toSet()
            )
        } else {
            emptyMap()
        }

    private suspend fun refreshThumbnailId(id: ThumbnailId.Legacy): ThumbnailId.File =
        getLink(
            fileId = id.fileId,
            refresh = flowOf(true),
        )
            .toResult()
            .getOrThrow()
            .getThumbnailId(id.volumeId, id.type) as ThumbnailId.File
}
