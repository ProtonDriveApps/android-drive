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

package me.proton.core.drive.db.test

import me.proton.android.drive.db.DriveDatabase
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.db.entity.LinkEntity
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity
import me.proton.core.drive.share.data.db.ShareEntity
import me.proton.core.user.data.entity.UserEntity

data class FolderContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val share: ShareEntity,
    val link: LinkEntity,
    val parent: LinkEntity? = null,
) : BaseContext()

data class FileContext(
    val db: DriveDatabase,
    val user: UserEntity,
    val share: ShareEntity,
    val link: LinkEntity,
    val parent: LinkEntity,
    val revisionId: String
) : BaseContext()

suspend fun ShareContext.folder(
    id: String,
    block: suspend FolderContext.() -> Unit = {},
) {
    folder(
        link = NullableLinkEntity(id = id, type = 1L),
        block = block,
    )
}

suspend fun ShareContext.folder(
    link: LinkEntity,
    block: suspend FolderContext.() -> Unit,
) {
    db.driveLinkDao.insertOrUpdate(link)
    FolderContext(db, user, share, link).block()
}


suspend fun FolderContext.folder(id: String, block: suspend FolderContext.() -> Unit = {}) {
    folder(
        link = NullableLinkEntity(id = id, parentId = link.id, type = 1L),
        block = block
    )
}

suspend fun FolderContext.folder(
    link: LinkEntity,
    block: suspend FolderContext.() -> Unit,
) {
    db.driveLinkDao.insertOrUpdate(link)
    FolderContext(db, user, share, link, this.link).block()
}

suspend fun FolderContext.file(
    id: String,
    block: suspend FileContext.() -> Unit = {},
) {
    file(
        link = NullableLinkEntity(
            id = id,
            parentId = this.link.id,
            type = 2L
        ),
        properties = LinkFilePropertiesEntity(
            userId = user.userId,
            shareId = share.id,
            linkId = id,
            activeRevisionId = "revision-$id",
            hasThumbnail = false,
            contentKeyPacket = "",
            contentKeyPacketSignature = null,
            activeRevisionSignatureAddress = null,
        ),
        block = block,
    )
}

suspend fun FolderContext.file(
    link: LinkEntity,
    properties: LinkFilePropertiesEntity,
    block: suspend FileContext.() -> Unit = {}
) {
    db.driveLinkDao.insertOrUpdate(link)
    db.driveLinkDao.insertOrUpdate(properties)
    FileContext(db, user, share, link, this.link, properties.activeRevisionId).block()
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
)

@Suppress("FunctionName")
private fun FolderContext.NullableLinkEntity(
    id: String,
    parentId: String?,
    type: Long
) = NullableLinkEntity(
    userId = user.userId,
    shareId = share.id,
    id = id,
    parentId = parentId,
    type = type,
)

@Suppress("FunctionName")
private fun NullableLinkEntity(
    userId: UserId,
    shareId: String,
    id: String,
    parentId: String?,
    type: Long
) = LinkEntity(
    id = id,
    shareId = shareId,
    userId = userId,
    parentId = parentId,
    type = type,
    name = id,
    nameSignatureEmail = "",
    hash = "",
    state = type,
    expirationTime = null,
    size = type,
    mimeType = "",
    attributes = type,
    permissions = type,
    nodeKey = "",
    nodePassphrase = "",
    nodePassphraseSignature = "",
    signatureAddress = "",
    creationTime = type,
    lastModified = type,
    trashedTime = null,
    shared = type,
    numberOfAccesses = type,
    shareUrlExpirationTime = null,
    xAttr = null,
    shareUrlShareId = null,
    shareUrlId = null,
)
