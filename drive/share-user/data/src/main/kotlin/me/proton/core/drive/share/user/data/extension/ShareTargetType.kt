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

package me.proton.core.drive.share.user.data.extension

import me.proton.core.drive.share.user.data.api.entities.ShareTargetTypeDto
import me.proton.core.drive.share.user.domain.entity.ShareTargetType

fun ShareTargetType.toShareTargetTypeDto(): Long = when (this) {
    ShareTargetType.Root -> error("Root type is not supported")
    ShareTargetType.Folder -> ShareTargetTypeDto.FOLDER
    ShareTargetType.File -> ShareTargetTypeDto.FILE
    ShareTargetType.Album -> ShareTargetTypeDto.ALBUM
    ShareTargetType.Photo -> ShareTargetTypeDto.PHOTO
    ShareTargetType.Document -> ShareTargetTypeDto.DOCUMENT
}

fun Set<ShareTargetType>.toShareTargetTypeDtos(): Set<Long> =
    map { type -> type.toShareTargetTypeDto() }.toSet()
