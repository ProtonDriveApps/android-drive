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
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.link.data.db.entity.LinkWithProperties
import me.proton.core.drive.share.domain.entity.ShareId

private fun LinkDto.toLinkEntity(shareId: ShareId) =
    LinkEntity(
        id = id,
        shareId = shareId.id,
        userId = shareId.userId,
        parentId = parentId,
        type = type,
        name = name,
        nameSignatureEmail = nameSignatureEmail,
        hash = hash,
        state = state,
        expirationTime = expirationTime,
        size = size,
        mimeType = mimeType,
        attributes = attributes,
        permissions = permissions,
        nodeKey = nodeKey,
        nodePassphrase = nodePassphrase,
        nodePassphraseSignature = nodePassphraseSignature,
        signatureAddress = signatureAddress,
        creationTime = creationTime,
        lastModified = lastModificationTime,
        trashedTime = trashed,
        shared = shared,
        numberOfAccesses = sharingDetails?.shareUrl?.numberOfAccesses ?: 0,
        shareUrlExpirationTime = sharingDetails?.shareUrl?.expirationTime,
        xAttr = xAttr,
        sharingDetailsShareId = sharingDetails?.shareId,
        shareUrlId = sharingDetails?.shareUrl?.shareUrlId,
    )


fun LinkDto.toLinkWithProperties(shareId: ShareId): LinkWithProperties {
    val linkEntity = toLinkEntity(shareId)
    val folderProperties = this.folderProperties
    val fileProperties = this.fileProperties
    return when {
        folderProperties != null ->
            LinkWithProperties(
                linkEntity,
                folderProperties.toLinkFolderPropertiesEntity(linkEntity.userId, linkEntity.shareId, linkEntity.id)
            )
        fileProperties != null ->
            LinkWithProperties(
                linkEntity,
                fileProperties.toLinkFilePropertiesEntity(linkEntity.userId, linkEntity.shareId, linkEntity.id)
            )
        else -> throw IllegalStateException("Link should have either file or folder properties")
    }
}
