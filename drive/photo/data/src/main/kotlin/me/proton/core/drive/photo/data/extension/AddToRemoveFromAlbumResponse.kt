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

package me.proton.core.drive.photo.data.extension

import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.photo.data.api.response.AddToRemoveFromAlbumResponse
import me.proton.core.drive.photo.domain.entity.AddToRemoveFromAlbumResult

fun List<AddToRemoveFromAlbumResponse.Responses>.toAddToRemoveFromAlbumResult() =
        map { response ->
            if (response.response.code == ProtonApiCode.SUCCESS) {
                AddToRemoveFromAlbumResult.AddRemovePhotoResult.Success(response.linkId)
            } else {
                AddToRemoveFromAlbumResult.AddRemovePhotoResult.Error(
                    fileId = response.linkId,
                    code = response.response.code,
                    error = response.response.error,
                )
            }
        }
        .let { results ->
            AddToRemoveFromAlbumResult(results)
        }
