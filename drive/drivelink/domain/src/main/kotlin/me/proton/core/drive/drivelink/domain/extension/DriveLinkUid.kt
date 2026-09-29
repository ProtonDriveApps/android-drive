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

package me.proton.core.drive.drivelink.domain.extension

import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.AlbumContext
import me.proton.core.drive.link.domain.entity.FileContext
import me.proton.core.drive.link.domain.entity.FolderContext
import me.proton.core.drive.link.domain.entity.NodeContext
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.revisionUid
import me.proton.core.drive.link.domain.extension.userId
import me.proton.drive.sdk.entity.LegacyParentNodeUid
import me.proton.drive.sdk.entity.NodeUid
import me.proton.drive.sdk.entity.ParentNodeUid
import me.proton.drive.sdk.entity.RevisionUid

val DriveLink.nodeUid: NodeUid get() = id.nodeUid(volumeId)

val DriveLink.nodeContext: NodeContext get() = when (this) {
    is DriveLink.File -> fileContext
    is DriveLink.Folder -> folderContext
    is DriveLink.Album -> albumContext
}

val DriveLink.File.fileContext: FileContext get() =
    FileContext(id.userId, nodeUid, volumeType)

val DriveLink.Folder.folderContext: FolderContext get() =
    FolderContext(id.userId, nodeUid, volumeType)

val DriveLink.Album.albumContext: AlbumContext get() =
    AlbumContext(id.userId, nodeUid, volumeType)

val DriveLink.File.revisionUid: RevisionUid get() = link.revisionUid(volumeId)

val DriveLink.Folder.parentFolderUid: ParentNodeUid get() = LegacyParentNodeUid(
    volumeId = volumeId.id,
    linkId = id.id,
)
