/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.files.presentation.component.files

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
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.volume.domain.entity.VolumeId

internal val PREVIEW_LINK = Link.File(
    id = FileId(ShareId(UserId("USER_ID"), "SHARE_ID"), "FILE_ID"),
    parentId = FolderId(ShareId(UserId("USER_ID"), "SHARE_ID"), "PARENT_ID"),
    name = "revision_id",
    size = Bytes(0L),
    lastModified = TimestampS(0),
    mimeType = "text/plain",
    isShared = false,
    key = "",
    passphrase = "",
    passphraseSignature = "",
    numberOfAccesses = 0L,
    uploadedBy = "He-Who-Must-Not-Be-Named",
    isFavorite = false,
    hasThumbnail = false,
    activeRevisionId = "",
    xAttr = null,
    contentKeyPacket = "",
    contentKeyPacketSignature = "",
    attributes = Attributes(0),
    permissions = Permissions(0),
    state = Link.State.ACTIVE,
    nameSignatureEmail = "",
    hash = "",
    expirationTime = null,
    nodeKey = "",
    nodePassphrase = "",
    nodePassphraseSignature = "",
    signatureEmail = "",
    creationTime = TimestampS(0),
    trashedTime = null,
    shareUrlExpirationTime = null,
    sharingDetails = null,
)
internal val PREVIEW_LINK_FOLDER = Link.Folder(
    id = FolderId(ShareId(UserId("USER_ID"), "SHARE_ID"), "FOLDER_ID"),
    parentId = FolderId(ShareId(UserId("USER_ID"), "SHARE_ID"), "PARENT_ID"),
    name = "revision_id",
    size = Bytes(0L),
    lastModified = TimestampS(0),
    mimeType = "text/plain",
    isShared = false,
    key = "",
    passphrase = "",
    passphraseSignature = "",
    numberOfAccesses = 0L,
    uploadedBy = "He-Who-Must-Not-Be-Named",
    isFavorite = false,
    xAttr = null,
    attributes = Attributes(0),
    permissions = Permissions(0),
    state = Link.State.ACTIVE,
    nameSignatureEmail = "",
    hash = "",
    expirationTime = null,
    nodeKey = "",
    nodePassphrase = "",
    nodePassphraseSignature = "",
    signatureEmail = "",
    creationTime = TimestampS(0),
    trashedTime = null,
    shareUrlExpirationTime = null,
    sharingDetails = null,
    nodeHashKey = "",
)
internal val PREVIEW_DRIVELINK = DriveLink.File(
    link = PREVIEW_LINK,
    volumeId = VolumeId("VOLUME_ID"),
    isMarkedAsOffline = false,
    isAnyAncestorMarkedAsOffline = false,
    downloadState = null,
    trashState = null,
    shareInvitationCount = null,
    shareMemberCount = null,
    shareUser = null,
)
internal val PREVIEW_DRIVELINK_FOLDER = DriveLink.Folder(
    link = PREVIEW_LINK_FOLDER,
    volumeId = VolumeId("VOLUME_ID"),
    isMarkedAsOffline = false,
    isAnyAncestorMarkedAsOffline = false,
    downloadState = null,
    trashState = null,
    shareInvitationCount = null,
    shareMemberCount = null,
    shareUser = null,
)
internal val PREVIEW_DRIVELINK_SHARED_WITH_ME = PREVIEW_DRIVELINK.copy(
    cryptoName = CryptoProperty.Decrypted("debug-log.txt", VerificationStatus.Success),
    shareUser = ShareUser.Member(
        id = "MEMBER_ID",
        inviter = "inviter@example.com",
        email = "me@example.com",
        createTime = TimestampS(1720000800),
        permissions = Permissions.editor,
        displayName = "Inviter",
        keyPacket = "",
        keyPacketSignature = "",
        sessionKeySignature = "",
    )
)

internal val PREVIEW_DRIVELINK_FOLDER_SHARED_WITH_ME = PREVIEW_DRIVELINK_FOLDER.copy(
    cryptoName = CryptoProperty.Decrypted("Pictures", VerificationStatus.Success),
    shareUser = ShareUser.Member(
        id = "MEMBER_ID",
        inviter = "inviter@example.com",
        email = "me@example.com",
        createTime = TimestampS(1720000800),
        permissions = Permissions.editor,
        displayName = "Inviter",
        keyPacket = "",
        keyPacketSignature = "",
        sessionKeySignature = "",
    )
)
