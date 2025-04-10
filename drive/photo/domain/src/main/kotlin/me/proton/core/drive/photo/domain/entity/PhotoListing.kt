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

package me.proton.core.drive.photo.domain.entity

import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.LinkId


sealed interface PhotoListing {
    val linkId: FileId
    val captureTime: TimestampS
    val nameHash: String?
    val contentHash: String?

    data class Volume(
        override val linkId: FileId,
        override val captureTime: TimestampS,
        override val nameHash: String?,
        override val contentHash: String?,
    ) : PhotoListing

    data class Album(
        override val linkId: FileId,
        override val captureTime: TimestampS,
        override val nameHash: String?,
        override val contentHash: String?,
        val albumId: AlbumId,
        val addedTime: TimestampS,
        val isChildOfAlbum: Boolean,
    ) : PhotoListing {

        enum class SortBy {
            CAPTURED,
            ADDED,
        }
    }
}
