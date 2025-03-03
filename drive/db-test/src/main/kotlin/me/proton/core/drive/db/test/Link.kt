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

package me.proton.core.drive.db.test

import me.proton.android.drive.db.DriveDatabase
import me.proton.core.account.data.entity.AccountEntity
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.db.entity.LinkAlbumPropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity
import me.proton.core.drive.link.data.db.entity.LinkFolderPropertiesEntity
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.photo.data.db.entity.PhotoListingEntity
import me.proton.core.drive.share.data.api.ShareDto
import me.proton.core.drive.share.data.db.ShareEntity
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.data.db.VolumeEntity
import me.proton.core.user.data.entity.UserEntity

data class FolderContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val account: AccountEntity,
    val volume: VolumeEntity,
    val share: ShareEntity,
    val link: LinkEntity,
    val parent: LinkEntity? = null,
) : BaseContext()

data class FileContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val account: AccountEntity,
    val volume: VolumeEntity,
    val share: ShareEntity,
    val link: LinkEntity,
    val parent: LinkEntity,
    val revisionId: String,
) : BaseContext()

data class AlbumContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val account: AccountEntity,
    val volume: VolumeEntity,
    val share: ShareEntity,
    val link: LinkEntity,
    val parent: LinkEntity,
) : BaseContext()

suspend fun ShareContext.folder(
    id: String,
    block: suspend FolderContext.() -> Unit,
): FolderId {
    folder(
        link = NullableLinkEntity(id = id, type = 1L),
        properties = LinkFolderPropertiesEntity(
            userId = user.userId,
            shareId = share.id,
            linkId = id,
            nodeHashKey = "node-hash-key-$id"
        ),
        block = block,
    )
    return FolderId(ShareId(user.userId, share.id), id)
}

suspend fun ShareContext.folder(
    link: LinkEntity,
    properties: LinkFolderPropertiesEntity,
    block: suspend FolderContext.() -> Unit,
) {
    db.driveLinkDao.insertOrUpdate(link)
    db.driveLinkDao.insertOrUpdate(properties)
    FolderContext(db, user, account, volume, share, link).block()
}


suspend fun FolderContext.folder(
    id: String,
    sharingDetailsShareId: String? = null,
    block: suspend FolderContext.() -> Unit = {},
): FolderId {
    folder(
        link = NullableLinkEntity(
            id = id,
            parentId = link.id,
            type = 1L,
            sharingDetailsShareId = sharingDetailsShareId,
        ),
        block = block
    )
    return FolderId(ShareId(user.userId, share.id), id)
}

suspend fun FolderContext.folder(
    link: LinkEntity,
    properties: LinkFolderPropertiesEntity = LinkFolderPropertiesEntity(
        userId = user.userId,
        shareId = share.id,
        linkId = link.id,
        nodeHashKey = "node-hash-key-${link.id}"
    ),
    block: suspend FolderContext.() -> Unit,
) {
    db.driveLinkDao.insertOrUpdate(link)
    db.driveLinkDao.insertOrUpdate(properties)
    FolderContext(db, user, account, volume, share, link, this.link).block()
}

suspend fun FolderContext.file(
    id: String,
    sharingDetailsShareId: String? = null,
    block: suspend FileContext.() -> Unit = {},
): FileId {
    file(
        link = NullableLinkEntity(
            id = id,
            parentId = this.link.id,
            type = 2L,
            sharingDetailsShareId = sharingDetailsShareId,
        ),
        block = block,
    )
    return FileId(ShareId(user.userId, share.id), id)
}

suspend fun FolderContext.file(
    link: LinkEntity,
    properties: LinkFilePropertiesEntity = LinkFilePropertiesEntity(
        userId = user.userId,
        shareId = share.id,
        linkId = link.id,
        activeRevisionId = "revision-${link.id}",
        hasThumbnail = false,
        contentKeyPacket = "",
        contentKeyPacketSignature = null,
        activeRevisionSignatureAddress = null,
    ),
    block: suspend FileContext.() -> Unit = {},
) {
    db.driveLinkDao.insertOrUpdate(link)
    db.driveLinkDao.insertOrUpdate(properties)
    if (share.type == ShareDto.TYPE_PHOTO) {
        db.photoListingDao.insertOrUpdate(
            PhotoListingEntity(
                userId = user.userId,
                volumeId = volume.id,
                shareId = share.id,
                linkId = link.id,
                captureTime = 0,
                hash = null,
                contentHash = null,
                mainPhotoLinkId = null,
            )
        )
    }
    FileContext(db, user, account, volume, share, link, this.link, properties.activeRevisionId).block()
}

suspend fun FolderContext.album(
    id: String,
    block: suspend AlbumContext.() -> Unit,
): AlbumId {
    album(
        link = NullableLinkEntity(id = id, parentId= this.link.id, type = 3L),
        properties = LinkAlbumPropertiesEntity(
            userId = user.userId,
            shareId = share.id,
            linkId = id,
            nodeHashKey = "node-hash-key-$id",
            locked = false,
            lastActivityTime = 0L,
            photoCount = 0L,
            coverLinkId = null,
        ),
        block = block,
    )
    return AlbumId(ShareId(user.userId, share.id), id)
}

suspend fun FolderContext.album(
    link: LinkEntity,
    properties: LinkAlbumPropertiesEntity = LinkAlbumPropertiesEntity(
        userId = user.userId,
        shareId = share.id,
        linkId = link.id,
        nodeHashKey = "node-hash-key-${link.id}",
        locked = false,
        lastActivityTime = 0L,
        photoCount = 0L,
        coverLinkId = null,
    ),
    block: suspend AlbumContext.() -> Unit,
) {
    db.driveLinkDao.insertOrUpdate(link)
    db.driveLinkDao.insertOrUpdate(properties)
    AlbumContext(db, user, account, volume,share, link, this.link).block()
}

@Suppress("FunctionName")
private fun ShareContext.NullableLinkEntity(
    id: String,
    type: Long,
    parentId: String? = null,
) = NullableLinkEntity(
    userId = user.userId,
    shareId = share.id,
    id = id,
    parentId = parentId,
    type = type,
    signatureAddress = account.email ?: "",
)

@Suppress("FunctionName")
private fun FolderContext.NullableLinkEntity(
    id: String,
    parentId: String?,
    type: Long,
    sharingDetailsShareId: String? = null
) = NullableLinkEntity(
    userId = user.userId,
    shareId = share.id,
    id = id,
    parentId = parentId,
    type = type,
    sharingDetailsShareId = sharingDetailsShareId,
    signatureAddress = account.email ?: "",
)

@Suppress("FunctionName")
private fun NullableLinkEntity(
    userId: UserId,
    shareId: String,
    id: String,
    parentId: String?,
    type: Long,
    signatureAddress: String,
    state: Long = LinkDto.STATE_ACTIVE,
    sharingDetailsShareId: String? = null,
) = LinkEntity(
    id = id,
    shareId = shareId,
    userId = userId,
    parentId = parentId,
    type = type,
    name = id,
    nameSignatureEmail = "",
    hash = "",
    state = state,
    expirationTime = null,
    size = type,
    mimeType = "",
    attributes = type,
    permissions = type,
    nodeKey = "node-key-$id",
    nodePassphrase = "l".repeat(32),
    nodePassphraseSignature = "",
    signatureAddress = signatureAddress,
    creationTime = type,
    lastModified = type,
    trashedTime = null,
    shared = type,
    numberOfAccesses = type,
    shareUrlExpirationTime = null,
    xAttr = null,
    sharingDetailsShareId = sharingDetailsShareId,
    shareUrlId = null,
)
