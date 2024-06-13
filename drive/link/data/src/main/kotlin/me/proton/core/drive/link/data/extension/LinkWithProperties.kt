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

import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkFolderPropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkWithProperties
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.share.domain.entity.ShareId

fun LinkWithProperties.toLink(): Link = when (properties) {
    is LinkFilePropertiesEntity -> Link.File(
        id = FileId(ShareId(link.userId, link.shareId), link.id),
        parentId = link.parentId?.let { FolderId(ShareId(link.userId, link.shareId), it) },
        name = link.name,
        size = Bytes(link.size),
        lastModified = TimestampS(link.lastModified),
        mimeType = link.mimeType,
        isShared = link.isShared,
        key = link.nodeKey,
        passphrase = link.nodePassphrase,
        passphraseSignature = link.nodePassphraseSignature,
        numberOfAccesses = link.numberOfAccesses,
        uploadedBy = properties.activeRevisionSignatureAddress ?: link.nameSignatureEmail ?: link.signatureAddress,
        hasThumbnail = properties.hasThumbnail,
        activeRevisionId = properties.activeRevisionId,
        contentKeyPacket = properties.contentKeyPacket,
        contentKeyPacketSignature = properties.contentKeyPacketSignature,
        isFavorite = false,
        attributes = Attributes(link.attributes),
        permissions = Permissions(link.permissions),
        state = link.state.toState(),
        nameSignatureEmail = link.nameSignatureEmail,
        hash = link.hash,
        expirationTime = link.expirationTime?.let { expirationTime -> TimestampS(expirationTime) },
        nodeKey = link.nodeKey,
        nodePassphrase = link.nodePassphrase,
        nodePassphraseSignature = link.nodePassphraseSignature,
        signatureAddress = link.signatureAddress,
        creationTime = TimestampS(link.creationTime),
        trashedTime = link.trashedTime?.let { trashedTime -> TimestampS(trashedTime) },
        shareUrlExpirationTime = link.shareUrlExpirationTime?.let { shareUrlExpirationTime ->
            TimestampS(shareUrlExpirationTime)
        },
        xAttr = link.xAttr,
        sharingDetails = link.sharingDetails(),
        photoCaptureTime = properties.photoCaptureTime?.let { captureTime -> TimestampS(captureTime) },
        photoContentHash = properties.photoContentHash,
        mainPhotoLinkId = properties.mainPhotoLinkId,
        defaultThumbnailId = properties.defaultThumbnailId,
        photoThumbnailId = properties.photoThumbnailId,
    )
    is LinkFolderPropertiesEntity -> Link.Folder(
        id = FolderId(ShareId(link.userId, link.shareId), link.id),
        parentId = link.parentId?.let { parentId -> FolderId(ShareId(link.userId, link.shareId), parentId) },
        name = link.name,
        size = Bytes(link.size),
        lastModified = TimestampS(link.lastModified),
        mimeType = link.mimeType,
        isShared = link.isShared,
        key = link.nodeKey,
        passphrase = link.nodePassphrase,
        passphraseSignature = link.nodePassphraseSignature,
        numberOfAccesses = link.numberOfAccesses,
        uploadedBy = link.signatureAddress,
        nodeHashKey = properties.nodeHashKey,
        isFavorite = false,
        attributes = Attributes(link.attributes),
        permissions = Permissions(link.permissions),
        state = link.state.toState(),
        nameSignatureEmail = link.nameSignatureEmail,
        hash = link.hash,
        expirationTime = link.expirationTime?.let { expirationTime -> TimestampS(expirationTime) },
        nodeKey = link.nodeKey,
        nodePassphrase = link.nodePassphrase,
        nodePassphraseSignature = link.nodePassphraseSignature,
        signatureAddress = link.signatureAddress,
        creationTime = TimestampS(link.creationTime),
        trashedTime = link.trashedTime?.let { trashedTime -> TimestampS(trashedTime) },
        shareUrlExpirationTime = link.shareUrlExpirationTime?.let { shareUrlExpirationTime ->
            TimestampS(shareUrlExpirationTime)
        },
        xAttr = link.xAttr,
        sharingDetails = link.sharingDetails(),
    )
}

private fun Long.toState() = when (this) {
    LinkDto.STATE_DRAFT -> Link.State.DRAFT
    LinkDto.STATE_ACTIVE -> Link.State.ACTIVE
    LinkDto.STATE_TRASHED -> Link.State.TRASHED
    LinkDto.STATE_DELETED -> Link.State.DELETED
    LinkDto.STATE_RESTORING -> Link.State.RESTORING
    else -> throw IllegalArgumentException("Unknown state $this")
}
