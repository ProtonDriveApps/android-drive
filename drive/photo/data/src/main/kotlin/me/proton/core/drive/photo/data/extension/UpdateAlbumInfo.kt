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

import me.proton.core.drive.photo.data.api.request.UpdateAlbumRequest
import me.proton.core.drive.photo.domain.entity.UpdateAlbumInfo

fun UpdateAlbumInfo.toUpdateAlbumRequest() =
    UpdateAlbumRequest(
        coverLinkId = coverLinkId?.id,
        link = takeIf {
            name != null ||
            hash != null ||
            nameSignatureEmail != null ||
            originalHash != null ||
            xAttr != null
        }
            ?.let {
                UpdateAlbumRequest.LinkDto(
                    name = name,
                    hash = hash,
                    nameSignatureEmail = nameSignatureEmail,
                    originalHash = originalHash,
                    xAttr = xAttr,
                )
            },
    )
