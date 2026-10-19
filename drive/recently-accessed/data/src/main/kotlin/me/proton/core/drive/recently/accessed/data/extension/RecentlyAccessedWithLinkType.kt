/*
 * Copyright (c) 2026 Proton AG.
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

package me.proton.core.drive.recently.accessed.data.extension

import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.recently.accessed.data.db.entity.RecentlyAccessedEntity
import me.proton.core.drive.recently.accessed.data.db.entity.RecentlyAccessedWithLinkType
import me.proton.core.drive.recently.accessed.domain.entity.RecentlyAccessed
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun RecentlyAccessedWithLinkType.toRecentlyAccessed() = RecentlyAccessed(
    linkId = entity.toLinkId(linkType),
    volumeId = VolumeId(entity.volumeId),
    accessTime = TimestampMs(entity.accessTime),
)

private fun RecentlyAccessedEntity.toLinkId(
    linkType: Long,
): LinkId {
    val shareId = ShareId(userId, shareId)
    return when (linkType) {
        LinkDto.TYPE_FILE -> FileId(shareId, linkId)
        LinkDto.TYPE_FOLDER -> FolderId(shareId, linkId)
        LinkDto.TYPE_ALBUM -> AlbumId(shareId, linkId)
        else -> error("Unknown link type $linkType")
    }
}
