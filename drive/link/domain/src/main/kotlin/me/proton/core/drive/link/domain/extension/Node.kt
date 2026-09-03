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

package me.proton.core.drive.link.domain.extension

import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.drive.sdk.entity.AlbumNode
import me.proton.drive.sdk.entity.FileNode
import me.proton.drive.sdk.entity.FolderNode
import me.proton.drive.sdk.entity.Node
import me.proton.drive.sdk.entity.PhotoNode

fun Node.linkId(shareId: ShareId): LinkId = when (this) {
    is FileNode -> linkId(shareId)
    is FolderNode -> linkId(shareId)
    is PhotoNode -> linkId(shareId)
    is AlbumNode -> linkId(shareId)
}

fun FolderNode.linkId(shareId: ShareId) = FolderId(shareId, linkId)
fun FileNode.linkId(shareId: ShareId) = FileId(shareId, linkId)
fun PhotoNode.linkId(shareId: ShareId) = FileId(shareId, linkId)
fun AlbumNode.linkId(shareId: ShareId) = AlbumId(shareId, linkId)

private val Node.linkId: String get() = uid.linkId
