/*
 * Copyright (c) 2022-2024 Proton AG.
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

package me.proton.core.drive.linkupload.domain.extension

import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.linkId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.observability.domain.metrics.UploadInitiator
import me.proton.core.drive.volume.domain.entity.Volume
import me.proton.core.drive.volume.domain.extension.volumeId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.drive.sdk.entity.LegacyParentNodeUid
import me.proton.drive.sdk.entity.ParentNodeUid

val UploadFileLink.userId: UserId get() = parentFolderContext.userId

val UploadFileLink.volumeType: Volume.Type get() = parentFolderContext.volumeType

val UploadFileLink.volumeId: VolumeId get() = parentFolderContext.nodeUid.volumeId

val UploadFileLink.parentFolderUid: ParentNodeUid get() = LegacyParentNodeUid(
    volumeId = volumeId.id,
    linkId = parentFolderContext.nodeUid.linkId,
)

val UploadFileLink.parentLinkId: FolderId get() =
    FolderId(shareId, parentFolderContext.nodeUid.linkId)

val UploadFileLink.sizeOrZero get() = size ?: 0.bytes

fun UploadFileLink.toInitiator(): UploadInitiator = when (priority) {
    in 1..UploadFileLink.USER_PRIORITY -> UploadInitiator.explicit
    else -> UploadInitiator.background
}
