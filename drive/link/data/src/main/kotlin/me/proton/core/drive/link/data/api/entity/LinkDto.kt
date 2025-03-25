/*
 * Copyright (c) 2021-2023 Proton AG.
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
package me.proton.core.drive.link.data.api.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.drive.base.data.api.Dto.ACTIVE_URLS
import me.proton.core.drive.base.data.api.Dto.ALBUM_PROPERTIES
import me.proton.core.drive.base.data.api.Dto.ATTRIBUTES
import me.proton.core.drive.base.data.api.Dto.CREATE_TIME
import me.proton.core.drive.base.data.api.Dto.EXPIRATION_TIME
import me.proton.core.drive.base.data.api.Dto.FILE_PROPERTIES
import me.proton.core.drive.base.data.api.Dto.FOLDER_PROPERTIES
import me.proton.core.drive.base.data.api.Dto.HASH
import me.proton.core.drive.base.data.api.Dto.LINK_ID
import me.proton.core.drive.base.data.api.Dto.MIME_TYPE
import me.proton.core.drive.base.data.api.Dto.MODIFY_TIME
import me.proton.core.drive.base.data.api.Dto.NAME
import me.proton.core.drive.base.data.api.Dto.NAME_SIGNATURE_EMAIL
import me.proton.core.drive.base.data.api.Dto.NB_URLS
import me.proton.core.drive.base.data.api.Dto.NODE_KEY
import me.proton.core.drive.base.data.api.Dto.NODE_PASSPHRASE
import me.proton.core.drive.base.data.api.Dto.NODE_PASSPHRASE_SIGNATURE
import me.proton.core.drive.base.data.api.Dto.NUMBER_OF_ACCESSES
import me.proton.core.drive.base.data.api.Dto.PARENT_LINK_ID
import me.proton.core.drive.base.data.api.Dto.PERMISSIONS
import me.proton.core.drive.base.data.api.Dto.SHARED
import me.proton.core.drive.base.data.api.Dto.SHARE_ID
import me.proton.core.drive.base.data.api.Dto.SHARE_URL
import me.proton.core.drive.base.data.api.Dto.SHARE_URL_ID
import me.proton.core.drive.base.data.api.Dto.SHARING_DETAILS
import me.proton.core.drive.base.data.api.Dto.SIGNATURE_ADDRESS
import me.proton.core.drive.base.data.api.Dto.SIZE
import me.proton.core.drive.base.data.api.Dto.STATE
import me.proton.core.drive.base.data.api.Dto.TRASHED
import me.proton.core.drive.base.data.api.Dto.TYPE
import me.proton.core.drive.base.data.api.Dto.URLS_EXPIRED
import me.proton.core.drive.base.data.api.Dto.X_ATTR

@Serializable
data class LinkDto(
    @SerialName(LINK_ID)
    val id: String,
    @SerialName(PARENT_LINK_ID)
    val parentId: String?,
    @SerialName(TYPE)
    val type: Long,
    @SerialName(NAME)
    val name: String,
    @SerialName(NAME_SIGNATURE_EMAIL)
    val nameSignatureEmail: String?,
    @SerialName(HASH)
    val hash: String,
    @SerialName(STATE)
    val state: Long,
    @SerialName(EXPIRATION_TIME)
    val expirationTime: Long?,
    @SerialName(SIZE)
    val size: Long,
    @SerialName(MIME_TYPE)
    val mimeType: String,
    @SerialName(ATTRIBUTES)
    val attributes: Long,
    @SerialName(PERMISSIONS)
    val permissions: Long,
    @SerialName(NODE_KEY)
    val nodeKey: String,
    @SerialName(NODE_PASSPHRASE)
    val nodePassphrase: String,
    @SerialName(NODE_PASSPHRASE_SIGNATURE)
    val nodePassphraseSignature: String,
    @SerialName(SIGNATURE_ADDRESS)
    val signatureAddress: String?,
    @SerialName(CREATE_TIME)
    val creationTime: Long,
    @SerialName(MODIFY_TIME)
    val lastModificationTime: Long,
    @SerialName(TRASHED)
    val trashed: Long?,
    @SerialName(SHARED)
    val shared: Long,
    @SerialName(NB_URLS)
    val numberOfUrlsAttached: Long,
    @SerialName(ACTIVE_URLS)
    val numberOfActiveUrls: Long,
    @SerialName(URLS_EXPIRED)
    val allUrlsHaveExpired: Long,
    @SerialName(FILE_PROPERTIES)
    val fileProperties: LinkFilePropertiesDto?,
    @SerialName(FOLDER_PROPERTIES)
    val folderProperties: LinkFolderPropertiesDto?,
    @SerialName(ALBUM_PROPERTIES)
    val albumProperties: LinkAlbumPropertiesDto? = null,
    @SerialName(X_ATTR)
    val xAttr: String? = null,
    @SerialName(SHARING_DETAILS)
    val sharingDetails: SharingDetailsDto? = null,
) {

    @Serializable
    data class ShareUrlDto(
        @SerialName(SHARE_URL_ID)
        val shareUrlId: String,
        @SerialName(NUMBER_OF_ACCESSES)
        val numberOfAccesses: Long,
        @SerialName(EXPIRATION_TIME)
        val expirationTime: Long? = null,
    )

    @Serializable
    data class SharingDetailsDto(
        @SerialName(SHARE_ID)
        val shareId: String? = null,
        @SerialName(SHARE_URL)
        val shareUrl: ShareUrlDto? = null,
    )

    companion object {
        // region State
        const val STATE_DRAFT = 0L
        const val STATE_ACTIVE = 1L
        const val STATE_TRASHED = 2L
        const val STATE_DELETED = 3L
        const val STATE_RESTORING = 4L
        // endregion
    }
}
