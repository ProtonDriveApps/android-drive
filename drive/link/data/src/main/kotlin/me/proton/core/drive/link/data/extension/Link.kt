/*
 * Copyright (c) 2021-2024 Proton AG.
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

package me.proton.core.drive.link.data.extension

import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.db.entity.LinkAlbumPropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkFolderPropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkWithProperties
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.userId

fun Link.toLinkWithProperties() = LinkWithProperties(
    link = LinkEntity(
        id = id.id,
        shareId = id.shareId.id,
        userId = id.userId,
        parentId = parentId?.id,
        type = if (this is Link.Folder) 1L else 2L,
        name = name,
        nameSignatureEmail = nameSignatureEmail,
        hash = hash,
        state = state.toLong(),
        expirationTime = expirationTime?.value,
        size = size.value,
        mimeType = mimeType,
        attributes = attributes.value,
        permissions = permissions.value,
        nodeKey = nodeKey,
        nodePassphrase = nodePassphrase,
        nodePassphraseSignature = nodePassphraseSignature,
        signatureAddress = signatureEmail,
        creationTime = creationTime.value,
        lastModified = lastModified.value,
        trashedTime = trashedTime?.value,
        shared = if (isShared) 1L else 0L,
        numberOfAccesses = numberOfAccesses,
        shareUrlExpirationTime = shareUrlExpirationTime?.value,
        sharingDetailsShareId = sharingDetails?.shareId?.id,
        shareUrlId = sharingDetails?.shareUrlId?.id,
        xAttr = xAttr,
    ),
    properties = when (this) {
        is Link.File -> LinkFilePropertiesEntity(
            userId = id.userId,
            shareId = id.shareId.id,
            linkId = id.id,
            activeRevisionId = activeRevisionId,
            hasThumbnail = hasThumbnail,
            contentKeyPacket = contentKeyPacket,
            contentKeyPacketSignature = contentKeyPacketSignature,
            activeRevisionSignatureAddress = uploadedBy,
            photoCaptureTime = photoCaptureTime?.value,
            photoContentHash = photoContentHash,
            mainPhotoLinkId = mainPhotoLinkId,
            defaultThumbnailId = defaultThumbnailId,
            photoThumbnailId = photoThumbnailId,
        )
        is Link.Folder -> LinkFolderPropertiesEntity(
            userId = id.userId,
            shareId = id.shareId.id,
            linkId = id.id,
            nodeHashKey = nodeHashKey,
        )
        is Link.Album -> LinkAlbumPropertiesEntity(
            userId = id.userId,
            shareId = id.shareId.id,
            linkId = id.id,
            nodeHashKey = nodeHashKey,
            lastActivityTime = lastActivityTime.value,
            locked = isLocked,
            photoCount = photoCount,
            coverLinkId = coverLinkId?.id,
        )
    }
)

private fun Link.State.toLong() = when (this) {
    Link.State.DRAFT -> LinkDto.STATE_DRAFT
    Link.State.ACTIVE -> LinkDto.STATE_ACTIVE
    Link.State.TRASHED -> LinkDto.STATE_TRASHED
    Link.State.DELETED -> LinkDto.STATE_DELETED
    Link.State.RESTORING -> LinkDto.STATE_RESTORING
}
