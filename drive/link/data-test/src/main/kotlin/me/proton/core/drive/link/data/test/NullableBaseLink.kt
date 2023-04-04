/*
 * Copyright (c) 2023 Proton AG.
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

@file:Suppress("FunctionName")
/*
 * Copyright (c) 2023 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.core.drive.link.data.test

import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.link.domain.entity.File
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId

fun NullableFile(parentId: FolderId, filename: String = "test-file"): File {
    return NullableFile(id = FileId(parentId.shareId, filename), parentId = parentId)
}

fun NullableFile(
    id: FileId,
    parentId: FolderId,
    name: String = "",
    size: Bytes = Bytes(0L),
    lastModified: TimestampS = TimestampS(0L),
    mimeType: String = "",
    isShared: Boolean = false,
    key: String = "",
    passphrase: String = "",
    passphraseSignature: String = "",
    numberOfAccesses: Long = 0,
    shareUrlExpirationTime: TimestampS? = null,
    uploadedBy: String = "",
    isFavorite: Boolean = false,
    attributes: Attributes = Attributes(0L),
    permissions: Permissions = Permissions(0L),
    state: Link.State = Link.State.ACTIVE,
    nameSignatureEmail: String? = null,
    hash: String = "",
    expirationTime: TimestampS? = null,
    nodeKey: String = "",
    nodePassphrase: String = "",
    nodePassphraseSignature: String = "",
    signatureAddress: String = "",
    creationTime: TimestampS = TimestampS(0L),
    trashedTime: TimestampS? = null,
    hasThumbnail: Boolean = false,
    activeRevisionId: String = "",
    xAttr: String? = null,
    shareUrlId: ShareUrlId? = null,
    contentKeyPacket: String = "",
    contentKeyPacketSignature: String? = null,
): File {
    return Link.File(
        id = id,
        parentId = parentId,
        name = name,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        isShared = isShared,
        key = key,
        passphrase = passphrase,
        passphraseSignature = passphraseSignature,
        numberOfAccesses = numberOfAccesses,
        shareUrlExpirationTime = shareUrlExpirationTime,
        uploadedBy = uploadedBy,
        isFavorite = isFavorite,
        attributes = attributes,
        permissions = permissions,
        state = state,
        nameSignatureEmail = nameSignatureEmail,
        hash = hash,
        expirationTime = expirationTime,
        nodeKey = nodeKey,
        nodePassphrase = nodePassphrase,
        nodePassphraseSignature = nodePassphraseSignature,
        signatureAddress = signatureAddress,
        creationTime = creationTime,
        trashedTime = trashedTime,
        hasThumbnail = hasThumbnail,
        activeRevisionId = activeRevisionId,
        xAttr = xAttr,
        shareUrlId = shareUrlId,
        contentKeyPacket = contentKeyPacket,
        contentKeyPacketSignature = contentKeyPacketSignature,
    )
}

fun NullableFolder(parentId: FolderId, filename: String = "test-folder"): Folder {
    return NullableFolder(id = FolderId(parentId.shareId, filename), parentId = parentId)
}

fun NullableFolder(
    id: FolderId,
    parentId: FolderId,
    name: String = "",
    size: Bytes = Bytes(0L),
    lastModified: TimestampS = TimestampS(0L),
    mimeType: String = "",
    isShared: Boolean = false,
    key: String = "",
    passphrase: String = "",
    passphraseSignature: String = "",
    numberOfAccesses: Long = 0,
    shareUrlExpirationTime: TimestampS? = null,
    uploadedBy: String = "",
    isFavorite: Boolean = false,
    attributes: Attributes = Attributes(0L),
    permissions: Permissions = Permissions(0L),
    state: Link.State = Link.State.ACTIVE,
    nameSignatureEmail: String? = null,
    hash: String = "",
    expirationTime: TimestampS? = null,
    nodeKey: String = "",
    nodePassphrase: String = "",
    nodePassphraseSignature: String = "",
    signatureAddress: String = "",
    creationTime: TimestampS = TimestampS(0L),
    trashedTime: TimestampS? = null,
    xAttr: String? = null,
    shareUrlId: ShareUrlId? = null,
    nodeHashKey: String = "",
): Folder {
    return Link.Folder(
        id = id,
        parentId = parentId,
        name = name,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        isShared = isShared,
        key = key,
        passphrase = passphrase,
        passphraseSignature = passphraseSignature,
        numberOfAccesses = numberOfAccesses,
        shareUrlExpirationTime = shareUrlExpirationTime,
        uploadedBy = uploadedBy,
        isFavorite = isFavorite,
        attributes = attributes,
        permissions = permissions,
        state = state,
        nameSignatureEmail = nameSignatureEmail,
        hash = hash,
        expirationTime = expirationTime,
        nodeKey = nodeKey,
        nodePassphrase = nodePassphrase,
        nodePassphraseSignature = nodePassphraseSignature,
        signatureAddress = signatureAddress,
        creationTime = creationTime,
        trashedTime = trashedTime,
        xAttr = xAttr,
        shareUrlId = shareUrlId,
        nodeHashKey = nodeHashKey
    )
}