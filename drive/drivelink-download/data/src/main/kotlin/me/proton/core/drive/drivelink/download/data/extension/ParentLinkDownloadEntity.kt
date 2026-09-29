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
import me.proton.core.drive.link.domain.entity.AlbumContext
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileContext
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderContext
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.data.extension.toVolumeType
import me.proton.drive.sdk.entity.LegacyNodeUid

fun ParentLinkDownloadEntity.toDownloadParentLink() = DownloadParentLink(
    id = id,
    nodeContext = LegacyNodeUid(volumeId = volumeId, linkId = linkId).let { nodeUid ->
        when (linkType) {
            LinkDto.TYPE_FILE -> FileContext(userId, nodeUid, volumeType.toVolumeType())
            LinkDto.TYPE_FOLDER -> FolderContext(userId, nodeUid, volumeType.toVolumeType())
            LinkDto.TYPE_ALBUM -> AlbumContext(userId, nodeUid, volumeType.toVolumeType())
            else -> error("Unhandled link type: $linkType")
        }
    },
    linkId = ShareId(userId, shareId).let {
        when (linkType) {
            LinkDto.TYPE_FILE -> FileId(it, linkId)
            LinkDto.TYPE_FOLDER -> FolderId(it, linkId)
            LinkDto.TYPE_ALBUM -> AlbumId(it, linkId)
            else -> error("Unhandled link type: $linkType")
        }
    },
    priority = priority,
    retryable = retryable,
)
