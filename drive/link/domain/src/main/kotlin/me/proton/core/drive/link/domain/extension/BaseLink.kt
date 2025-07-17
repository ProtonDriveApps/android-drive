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

package me.proton.core.drive.link.domain.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.FileTypeCategory
import me.proton.core.drive.base.domain.entity.Timestamp
import me.proton.core.drive.base.domain.entity.toFileTypeCategory
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.share.domain.entity.ShareId

val List<BaseLink>.ids: List<LinkId> get() = map { link -> link.id }

val BaseLink.shareId: ShareId get() = id.shareId

val BaseLink.userId: UserId get() = shareId.userId

fun BaseLink.requireParentId(): ParentId = requireNotNull(parentId)

fun BaseLink.requireFolderId(): FolderId = requireNotNull(parentId as? FolderId)

fun BaseLink.requireAlbumId(): AlbumId = requireNotNull(parentId as? AlbumId)

val BaseLink.isSharedUrlExpired get() = shareUrlExpirationTime?.let { expirationTime ->
    expirationTime < Timestamp.now
} == true

val BaseLink.isProtonDocument: Boolean
    get() = mimeType.toFileTypeCategory() == FileTypeCategory.ProtonDoc

val BaseLink.isProtonSpreadsheet: Boolean
    get() = mimeType.toFileTypeCategory() == FileTypeCategory.ProtonSheet

val BaseLink.isProtonCloudFile: Boolean
    get() = mimeType.startsWith("application/vnd.proton.")

fun BaseLink.xAttrKey(sha256OfXAttr: String?): String
        = "xattr.${parentId?.id}.$sha256OfXAttr"
