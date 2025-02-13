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

package me.proton.core.drive.drivelink.sorting.domain.sorter

import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.CryptoProperty
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.drivelink.domain.entity.DriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.SharingDetails
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.shareurl.base.domain.entity.ShareUrlId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun file(
    name: String,
    type: String = "",
    lastModified: Long = 0L,
    size: Long = 0L,
) = driveLinkFile(
    name = name,
    type = type,
    lastModified = lastModified,
    size = size,
)

fun cryptedFile(
    name: String,
    type: String,
    lastModified: Long,
    size: Long,
) = driveLinkFile(
    name = name,
    type = type,
    lastModified = lastModified,
    size = size,
    cryptoName = CryptoProperty.Encrypted(name)
)

fun folder(
    name: String,
    lastModified: Long,
    size: Long,
) = driveLinkFolder(
    name = name,
    type = "Folder",
    lastModified = lastModified,
    size = size,
)

fun cryptedFolder(
    name: String,
    lastModified: Long,
    size: Long,
) = driveLinkFolder(
    name = name,
    type = "Folder",
    lastModified = lastModified,
    size = size,
    cryptoName = CryptoProperty.Encrypted(name),
)

private fun driveLinkFile(
    name: String,
    type: String,
    lastModified: Long,
    size: Long,
    cryptoName: CryptoProperty<String> = CryptoProperty.Decrypted(
        name,
        VerificationStatus.Success
    ),
): DriveLink.File {
    val userId = UserId("USER_ID")
    return DriveLink.File(
        link = Link.File(
            id = FileId(ShareId(userId, "SHARE_ID"), "ID"),
            parentId = FolderId(ShareId(userId, "SHARE_ID"), "PARENT_ID"),
            activeRevisionId = "revision",
            size = size.bytes,
            lastModified = TimestampS(lastModified),
            mimeType = type,
            numberOfAccesses = 2,
            isShared = true,
            uploadedBy = "m4@proton.black",
            hasThumbnail = false,
            name = name,
            key = "key",
            passphrase = "passphrase",
            passphraseSignature = "signature",
            contentKeyPacket = "contentKeyPacket",
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
            signatureEmail = "",
            creationTime = TimestampS(0),
            trashedTime = null,
            shareUrlExpirationTime = null,
            xAttr = null,
            sharingDetails = SharingDetails(
                ShareId(userId, ""),
                shareUrlId = ShareUrlId(ShareId(userId, ""), "")
            ),
        ),
        volumeId = VolumeId("VOLUME_ID"),
        isMarkedAsOffline = false,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = null,
        trashState = null,
        cryptoName = cryptoName,
        shareInvitationCount = null,
        shareMemberCount = null,
        shareUser = null,
    )
}


private fun driveLinkFolder(
    name: String,
    type: String,
    lastModified: Long,
    size: Long,
    cryptoName: CryptoProperty<String> = CryptoProperty.Decrypted(
        name,
        VerificationStatus.Success
    ),
): DriveLink.Folder {
    val userId = UserId("USER_ID")
    return DriveLink.Folder(
        link = Link.Folder(
            id = FolderId(ShareId(userId, "SHARE_ID"), "ID"),
            parentId = FolderId(ShareId(userId, "SHARE_ID"), "PARENT_ID"),
            size = size.bytes,
            lastModified = TimestampS(lastModified),
            mimeType = type,
            numberOfAccesses = 2,
            isShared = true,
            uploadedBy = "m4@proton.black",
            name = name,
            key = "key",
            passphrase = "passphrase",
            passphraseSignature = "signature",
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
            signatureEmail = "",
            creationTime = TimestampS(0),
            trashedTime = null,
            shareUrlExpirationTime = null,
            xAttr = null,
            sharingDetails = SharingDetails(
                ShareId(userId, ""),
                shareUrlId = ShareUrlId(ShareId(userId, ""), "")
            ),
            nodeHashKey = "",
        ),
        volumeId = VolumeId("VOLUME_ID"),
        isMarkedAsOffline = false,
        isAnyAncestorMarkedAsOffline = false,
        downloadState = null,
        trashState = null,
        cryptoName = cryptoName,
        shareInvitationCount = null,
        shareMemberCount = null,
        shareUser = null,
    )
}
