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

package me.proton.core.drive.drivelink.download.data.extension

import me.proton.core.drive.drivelink.download.data.db.entity.ParentLinkDownloadEntity
import me.proton.core.drive.drivelink.download.domain.entity.DownloadParentLink
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId

fun DownloadParentLink.toParentLinkDownloadEntity() = ParentLinkDownloadEntity(
    id = id,
    userId = linkId.userId,
    volumeId = volumeId.id,
    shareId = linkId.shareId.id,
    linkId = linkId.id,
    linkType = when (linkId) {
        is FileId -> LinkDto.TYPE_FILE
        is FolderId -> LinkDto.TYPE_FOLDER
        is AlbumId -> LinkDto.TYPE_ALBUM
    },
    priority = priority,
    retryable = retryable,
)
