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

package me.proton.core.drive.file.base.data.extension

import me.proton.core.drive.file.base.data.api.response.GetThumbnailsUrlsResponse
import me.proton.core.drive.file.base.domain.entity.ThumbnailId
import me.proton.core.drive.file.base.domain.entity.ThumbnailUrl

fun GetThumbnailsUrlsResponse.mapResults(thumbnailIds: Set<ThumbnailId>): Map<ThumbnailId, Result<ThumbnailUrl>> {
    val success = thumbnails
        .map { thumbnailDto -> thumbnailDto.toThumbnailUrl() }
        .associateBy { url -> url.id }
    val error = errors
        .map { thumbnailErrorDto -> thumbnailErrorDto.thumbnailId to thumbnailErrorDto.toApiException() }
        .associateBy { (thumbnailId, _) -> thumbnailId }
    return thumbnailIds.associateWith { thumbnailId ->
        when {
            success.containsKey(thumbnailId.id) -> Result.success(requireNotNull(success[thumbnailId.id]))
            error.containsKey(thumbnailId.id) -> Result.failure(requireNotNull(error[thumbnailId.id]).second)
            else -> Result.failure(IllegalStateException("Response did not contain id"))
        }
    }
}
