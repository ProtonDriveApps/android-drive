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

package me.proton.android.drive.photos.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult
import me.proton.core.drive.photo.domain.repository.AlbumRepository
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class RemovePhotosFromAlbum @Inject constructor(
    private val repository: AlbumRepository,
    private val getShare: GetShare,
    private val updateEventAction: UpdateEventAction,
) {

    suspend operator fun invoke(
        albumId: AlbumId,
        fileIds: List<FileId>,
    ): Result<AddToRemoveFromAlbumResult> = coRunCatching {
        updateEventAction(
            userId = albumId.userId,
            volumeId = getShare(albumId.shareId).toResult().getOrThrow().volumeId,
        ) {
            removeFromAlbum(
                albumId = albumId,
                fileIds = fileIds,
            )
        }
    }

    private suspend fun removeFromAlbum(
        albumId: AlbumId,
        fileIds: List<FileId>
    ): AddToRemoveFromAlbumResult = fileIds
        .groupBy({ linkId -> linkId.shareId }) { linkId -> linkId }
        .map { (shareId, linkIds) ->
            getShare(shareId).toResult().getOrThrow().volumeId to linkIds
        }
        .groupBy({ it.first} ) { it.second }
        .map { (volumeId, linkIds) ->
            repository.removeFromAlbum(
                volumeId = volumeId,
                albumId = albumId,
                linkIds = linkIds.flatten(),
            )
        }
        .map { addToRemoveFromAlbumResult -> addToRemoveFromAlbumResult.results }
        .let { addRemovePhotoResultList ->
            AddToRemoveFromAlbumResult(
                results = addRemovePhotoResultList.flatten(),
            )
        }
}
