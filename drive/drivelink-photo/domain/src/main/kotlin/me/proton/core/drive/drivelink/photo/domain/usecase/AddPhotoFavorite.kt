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

package me.proton.core.drive.drivelink.photo.domain.usecase

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.photo.CreatePhotoFavoriteInfo
import me.proton.core.drive.documentsprovider.domain.usecase.GetContentDigest
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.eventmanager.base.domain.usecase.UpdateEventAction
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.domain.repository.TagRepository
import me.proton.core.drive.photo.domain.usecase.GetRelatedPhotoIds
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import javax.inject.Inject

class AddPhotoFavorite @Inject constructor(
    private val repository: TagRepository,
    private val getRelatedPhotoIds: GetRelatedPhotoIds,
    private val getPhotoShare: GetPhotoShare,
    private val createPhotoFavoriteInfo: CreatePhotoFavoriteInfo,
    private val getContentDigest: GetContentDigest,
    private val updateEventAction: UpdateEventAction,
) {

    suspend operator fun invoke(photo: DriveLink.File) = coRunCatching {
        updateEventAction(photo.userId, photo.volumeId) {
            val parentId = photo.parentId
            repository.addFavorite(
                volumeId = photo.volumeId,
                fileId = photo.id,
                photoFavoriteInfo = if (parentId is AlbumId) {
                    val photoShare = getPhotoShare(photo.userId).toResult().getOrThrow()
                    val relatedPhotoIds = getRelatedPhotoIds(
                        volumeId = photo.volumeId,
                        albumId = parentId,
                        mainFileId = photo.id,
                    ).getOrThrow()
                    createPhotoFavoriteInfo(
                        photoId = photo.id,
                        shareId = photoShare.id,
                        contentDigestMap = (listOf(photo.id) + relatedPhotoIds).associateWith { id ->
                            getContentDigest(id).getOrNull()
                        },
                        relatedPhotoIds = relatedPhotoIds,
                    ).getOrThrow()
                } else {
                    null
                }
            )
        }
    }
}
