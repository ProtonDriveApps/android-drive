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

package me.proton.core.drive.file.base.domain.extension

import me.proton.core.drive.file.base.domain.entity.ThumbnailId
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun Link.File.getThumbnailIds(volumeId: VolumeId): Set<ThumbnailId> = when {
    !hasThumbnail -> emptySet()
    defaultThumbnailId == null && photoThumbnailId == null -> setOf(
        ThumbnailId.Legacy(
            volumeId = volumeId,
            fileId = id,
            revisionId = activeRevisionId,
        )
    )
    else -> setOfNotNull(
        defaultThumbnailId?.let { thumbnailId ->
            ThumbnailId.File(
                id = thumbnailId,
                userId = userId,
                volumeId = volumeId,
                type = ThumbnailType.DEFAULT,
            )
        },
        photoThumbnailId?.let { thumbnailId ->
            ThumbnailId.File(
                id = thumbnailId,
                userId = userId,
                volumeId = volumeId,
                type = ThumbnailType.PHOTO,
            )
        }
    )
}

fun Link.File.getThumbnailId(volumeId: VolumeId, thumbnailType: ThumbnailType): ThumbnailId? =
    getThumbnailIds(volumeId).firstOrNull { thumbnailId -> thumbnailId.type == thumbnailType }
