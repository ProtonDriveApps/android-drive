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

package me.proton.core.drive.photo.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.usecase.DeleteLinks
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class DeleteRelatedPhotoLinks @Inject constructor(
    private val getRelatedPhotoIds: GetRelatedPhotoIds,
    private val deleteLinks: DeleteLinks,
    private val getShare: GetShare,
) {
    suspend operator fun invoke(linkIds: List<FileId>): Result<Unit> = coRunCatching {
        linkIds.onEach { linkId ->
            getShare(linkId.shareId).toResult().getOrNull()?.let { share ->
                val relatedPhotoIds = getRelatedPhotoIds(share.volumeId, linkId).getOrThrow()
                deleteLinks(relatedPhotoIds).getOrThrow()
            }
        }
    }
}
