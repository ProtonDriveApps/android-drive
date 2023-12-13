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

import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.file.base.domain.entity.Thumbnail
import me.proton.core.drive.file.base.domain.entity.ThumbnailType

val ThumbnailType.fileName: String get() = when (this) {
    ThumbnailType.DEFAULT -> Thumbnail.default
    ThumbnailType.PHOTO -> Thumbnail.photo
}

val ThumbnailType.index: Long get() = when (this) {
    ThumbnailType.DEFAULT -> Block.THUMBNAIL_DEFAULT_INDEX
    ThumbnailType.PHOTO -> Block.THUMBNAIL_PHOTO_INDEX
}

fun ThumbnailType.toBlockType(): Block.Type = when (this) {
    ThumbnailType.DEFAULT -> Block.Type.THUMBNAIL_DEFAULT
    ThumbnailType.PHOTO -> Block.Type.THUMBNAIL_PHOTO
}
