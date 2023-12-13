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

package me.proton.core.drive.file.base.domain.entity

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.volume.domain.entity.VolumeId

sealed interface ThumbnailId {
    val id: String
    val userId: UserId
    val volumeId: VolumeId
    val type: ThumbnailType

    data class File(
        override val id: String,
        override val userId: UserId,
        override val volumeId: VolumeId,
        override val type: ThumbnailType
    ) : ThumbnailId

    data class Legacy(
        override val volumeId: VolumeId,
        val fileId: FileId,
        val revisionId: String,
    ) : ThumbnailId {
        override val id: String get() = error("Legacy thumbnails does not have id")
        override val userId: UserId = fileId.userId
        override val type: ThumbnailType = ThumbnailType.DEFAULT
    }
}
