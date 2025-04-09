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
package me.proton.core.drive.link.domain.entity

import kotlinx.serialization.Serializable
import me.proton.core.drive.base.domain.entity.Attributes
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.share.domain.entity.ShareId

sealed interface LinkId {
    val shareId: ShareId
    val id: String
}

sealed interface ParentId : LinkId

@Serializable
data class FileId(override val shareId: ShareId, override val id: String) : LinkId
@Serializable
data class FolderId(override val shareId: ShareId, override val id: String) : ParentId
@Serializable
data class AlbumId(override val shareId: ShareId, override val id: String) : ParentId

interface BaseLink {
    val id: LinkId
    val parentId: ParentId?
    val name: String
    val size: Bytes
    val lastModified: TimestampS
    val mimeType: String
    val isShared: Boolean
    val numberOfAccesses: Long
    val shareUrlExpirationTime: TimestampS?
    val uploadedBy: String
    val isFavorite: Boolean
    val xAttr: String?
}

interface File : BaseLink {
    override val id: FileId
    override val parentId: ParentId?
    val hasThumbnail: Boolean
    val activeRevisionId: String
    val photoCaptureTime: TimestampS?
}

interface Folder : BaseLink {
    override val id: FolderId
    val signatureEmail: String
}

interface Album : BaseLink {
    override val id: AlbumId
    val signatureEmail: String
    val isLocked: Boolean
    val coverLinkId: FileId?
    val lastActivityTime: TimestampS
    val photoCount: Long
}

sealed class Link : BaseLink {
    abstract val key: String
    abstract val passphrase: String
    abstract val passphraseSignature: String
    abstract val attributes: Attributes
    abstract val permissions: Permissions
    abstract val state: State
    abstract val nameSignatureEmail: String?
    abstract val hash: String
    abstract val expirationTime: TimestampS?
    abstract val nodeKey: String
    abstract val nodePassphrase: String
    abstract val nodePassphraseSignature: String
    abstract val signatureEmail: String
    abstract val creationTime: TimestampS
    abstract val trashedTime: TimestampS?
    abstract val sharingDetails: SharingDetails?

    data class File(
        override val id: FileId,
        override val parentId: ParentId?,
        override val name: String,
        override val size: Bytes,
        override val lastModified: TimestampS,
        override val mimeType: String,
        override val isShared: Boolean,
        override val key: String,
        override val passphrase: String,
        override val passphraseSignature: String,
        override val numberOfAccesses: Long,
        override val shareUrlExpirationTime: TimestampS?,
        override val uploadedBy: String,
        override val attributes: Attributes,
        override val permissions: Permissions,
        override val state: State,
        override val nameSignatureEmail: String?,
        override val hash: String,
        override val expirationTime: TimestampS?,
        override val nodeKey: String,
        override val nodePassphrase: String,
        override val nodePassphraseSignature: String,
        override val signatureEmail: String,
        override val creationTime: TimestampS,
        override val trashedTime: TimestampS?,
        override val hasThumbnail: Boolean,
        override val activeRevisionId: String,
        override val xAttr: String?,
        override val sharingDetails: SharingDetails?,
        val contentKeyPacket: String,
        val contentKeyPacketSignature: String?,
        override val photoCaptureTime: TimestampS? = null,
        val photoContentHash: String? = null,
        val mainPhotoLinkId: String? = null,
        val defaultThumbnailId: String? = null,
        val defaultThumbnailContentHash: String? = null,
        val photoThumbnailId: String? = null,
        val photoThumbnailContentHash: String? = null,
        val tags: List<PhotoTag> = emptyList(),
    ) : Link(), me.proton.core.drive.link.domain.entity.File {
        override val isFavorite: Boolean
            get() = PhotoTag.Favorites in tags
    }

    data class Folder(
        override val id: FolderId,
        override val parentId: ParentId?,
        override val name: String,
        override val size: Bytes,
        override val lastModified: TimestampS,
        override val mimeType: String,
        override val isShared: Boolean,
        override val key: String,
        override val passphrase: String,
        override val passphraseSignature: String,
        override val numberOfAccesses: Long,
        override val shareUrlExpirationTime: TimestampS?,
        override val uploadedBy: String,
        override val attributes: Attributes,
        override val permissions: Permissions,
        override val state: State,
        override val nameSignatureEmail: String?,
        override val hash: String,
        override val expirationTime: TimestampS?,
        override val nodeKey: String,
        override val nodePassphrase: String,
        override val nodePassphraseSignature: String,
        override val signatureEmail: String,
        override val creationTime: TimestampS,
        override val trashedTime: TimestampS?,
        override val xAttr: String?,
        override val sharingDetails: SharingDetails?,
        val nodeHashKey: String,
    ) : Link(), me.proton.core.drive.link.domain.entity.Folder {
        override val isFavorite: Boolean = false
    }

    data class Album(
        override val id: AlbumId,
        override val parentId: ParentId?,
        override val name: String,
        override val size: Bytes,
        override val lastModified: TimestampS,
        override val mimeType: String,
        override val isShared: Boolean,
        override val key: String,
        override val passphrase: String,
        override val passphraseSignature: String,
        override val numberOfAccesses: Long,
        override val shareUrlExpirationTime: TimestampS?,
        override val uploadedBy: String,
        override val attributes: Attributes,
        override val permissions: Permissions,
        override val state: State,
        override val nameSignatureEmail: String?,
        override val hash: String,
        override val expirationTime: TimestampS?,
        override val nodeKey: String,
        override val nodePassphrase: String,
        override val nodePassphraseSignature: String,
        override val signatureEmail: String,
        override val creationTime: TimestampS,
        override val trashedTime: TimestampS?,
        override val xAttr: String?,
        override val sharingDetails: SharingDetails?,
        val nodeHashKey: String,
        override val isLocked: Boolean,
        override val lastActivityTime: TimestampS,
        override val photoCount: Long,
        override val coverLinkId: FileId? = null,
    ) : Link(), me.proton.core.drive.link.domain.entity.Album {
        override val isFavorite: Boolean = false
    }

    enum class State {
        DRAFT, ACTIVE, TRASHED, DELETED, RESTORING
    }
}
