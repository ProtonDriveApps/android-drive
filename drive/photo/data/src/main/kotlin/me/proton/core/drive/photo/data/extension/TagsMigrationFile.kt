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

import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.photo.data.db.entity.TagsMigrationFileEntity
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile

fun TagsMigrationFile.toEntity() = TagsMigrationFileEntity(
    userId = fileId.userId,
    volumeId = volumeId.id,
    shareId = fileId.shareId.id,
    linkId = fileId.id,
    captureTime = captureTime.value,
    state = state,
    uriString = uriString,
    mimeType = mimeType,
)
