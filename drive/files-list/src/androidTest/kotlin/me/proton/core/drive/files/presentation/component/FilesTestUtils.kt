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

package me.proton.core.drive.files.presentation.component

import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

val BASE_FILE_LINK = Link.File(
    id = FileId(ShareId(UserId("ID"), "SHARE_ID"), "FILE_ID"),
    parentId = FolderId(ShareId(UserId("ID"),"SHARE_ID"), "PARENT_ID"),
    name = "FILE",
    size = Bytes(0),
    lastModified = TimestampS(0),
    mimeType = "text/plain",
    isShared = false,
    key = "KEY",
    passphrase = "PASSPHRASE",
    passphraseSignature = "PASSPHRASE_SIGNATURE",
    numberOfAccesses = 0,
    uploadedBy = "He-Who-Must-Not-Be-Named",
    hasThumbnail = false,
    activeRevisionId = "ACTIVE_REVISION",
    contentKeyPacket = "CONTENT_KEY_PACKET",
    contentKeyPacketSignature = null,
    isFavorite = false,
    attributes = Attributes(0),
    permissions = Permissions(0),
    state = Link.State.ACTIVE,
    nameSignatureEmail = "",
    hash = "",
    expirationTime = null,
    nodeKey = "",
    nodePassphrase = "",
    nodePassphraseSignature = "",
    signatureAddress = "",
    creationTime = TimestampS(0),
    trashedTime = null,
    shareUrlExpirationTime = null,
    xAttr = null,
    sharingDetails = null,
)

val BASE_FOLDER_LINK = Link.Folder(
    id = FolderId(ShareId(UserId("ID"),"SHARE_ID"), "FOLDER_ID"),
    parentId = FolderId(ShareId(UserId("ID"),"SHARE_ID"), "PARENT_ID"),
    name = "FOLDER",
    size = Bytes(0),
    lastModified = TimestampS(0),
    mimeType = "text/plain",
    isShared = false,
    key = "KEY",
    passphrase = "PASSPHRASE",
    passphraseSignature = "PASSPHRASE_SIGNATURE",
    numberOfAccesses = 0,
    uploadedBy = "He-Who-Must-Not-Be-Named",
    nodeHashKey = "NODE_HASH_KEY",
    isFavorite = false,
    attributes = Attributes(0),
    permissions = Permissions(0),
    state = Link.State.ACTIVE,
    nameSignatureEmail = "",
    hash = "",
    expirationTime = null,
    nodeKey = "",
    nodePassphrase = "",
    nodePassphraseSignature = "",
    signatureAddress = "",
    creationTime = TimestampS(0),
    trashedTime = null,
    shareUrlExpirationTime = null,
    xAttr = null,
    sharingDetails = null,
)

fun Link.toDriveLink() = when (this) {
    is Link.File -> DriveLink.File(
        link = this,
        volumeId = VolumeId("VOLUME_ID"),
        isMarkedAsOffline = false,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = null,
        trashState = null,
        cryptoName = CryptoProperty.Decrypted(name, VerificationStatus.Success),
        shareMemberCount = null,
        shareInvitationCount = null,
        shareUser = null,
    )
    is Link.Folder -> DriveLink.Folder(
        link = this,
        volumeId = VolumeId("VOLUME_ID"),
        isMarkedAsOffline = false,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = null,
        trashState = null,
        cryptoName = CryptoProperty.Decrypted(name, VerificationStatus.Success),
        shareInvitationCount = null,
        shareMemberCount = null,
        shareUser = null,
    )
}
