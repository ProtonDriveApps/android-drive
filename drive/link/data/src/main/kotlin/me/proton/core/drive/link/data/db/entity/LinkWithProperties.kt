/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.link.data.db.entity

import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.AlbumInfo
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.share.domain.entity.ShareId

sealed interface LinkPropertiesEntity

data class LinkWithProperties(
    val link: LinkEntity,
    val properties: LinkPropertiesEntity,
    val tags: List<Long> = emptyList(),
    val albumInfos: List<AlbumInfo> = emptyList(),
    val parentLinkType: Long? = null,
    val relatedPhotoIds: List<String> = emptyList(),
) {
    val linkId: LinkId
        get() = when (properties) {
            is LinkFilePropertiesEntity -> FileId(ShareId(link.userId, link.shareId), link.id)
            is LinkFolderPropertiesEntity -> FolderId(ShareId(link.userId, link.shareId), link.id)
            is LinkAlbumPropertiesEntity -> AlbumId(ShareId(link.userId, link.shareId), link.id)
        }
    val parentId: ParentId?
        get() = link.parentId?.let { linkParentId ->
            when (parentLinkType) {
                LinkDto.TYPE_ALBUM -> AlbumId(ShareId(link.userId, link.shareId), linkParentId)
                else -> FolderId(ShareId(link.userId, link.shareId), linkParentId)
            }
        }
}
