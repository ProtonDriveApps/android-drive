/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("FunctionName")
package me.proton.core.drive.test.entity

import me.proton.core.drive.link.data.api.entity.LinkActiveRevisionDto
import me.proton.core.drive.link.data.api.entity.LinkDto
import me.proton.core.drive.link.data.api.entity.LinkFilePropertiesDto
import me.proton.core.drive.link.data.api.entity.LinkFolderPropertiesDto

fun NullableFolderDto(
    id: String,
    parentId: String? = null,
    shareId: String? = null,
) =
    NullableLinkDto(
        id = id,
        type = 1,
        parentId = parentId,
        shareId = shareId,
        folderProperties = LinkFolderPropertiesDto(""),
    )

fun NullableFileDto(
    id: String,
    parentId: String? = null,
    shareId: String? = null,
) =
    NullableLinkDto(
        id = id,
        type = 2,
        parentId = parentId,
        shareId = shareId,
        fileProperties = LinkFilePropertiesDto(
            contentKeyPacket = "",
            contentKeyPacketSignature = null,
            activeRevision = LinkActiveRevisionDto(
                id = "revision-$id",
                creationTime = 0,
                size = 0,
                manifestSignature = "",
                signatureAddress = null,
                state = 0,
                thumbnail = 0,
                photo = null,
                thumbnails = emptyList(),
            )
        )
    )


fun NullableLinkDto(
    id: String,
    type: Long,
    parentId: String? = null,
    shareId: String? = null,
    fileProperties: LinkFilePropertiesDto? = null,
    folderProperties: LinkFolderPropertiesDto? = null,
) = LinkDto(
    id = id,
    parentId = parentId,
    type = type,
    name = "",
    nameSignatureEmail = null,
    hash = "",
    state = 0,
    expirationTime = null,
    size = 0,
    mimeType = "",
    attributes = 0,
    permissions = 0,
    nodeKey = "",
    nodePassphrase = "l".repeat(32),
    nodePassphraseSignature = "",
    signatureAddress = "",
    creationTime = 0,
    lastModificationTime = 0,
    trashed = null,
    shared = if (shareId != null) 1 else 0,
    numberOfUrlsAttached = 0,
    numberOfActiveUrls = 0,
    allUrlsHaveExpired = 0,
    fileProperties = fileProperties,
    folderProperties = folderProperties,
    xAttr = null,
    sharingDetails = shareId?.let { LinkDto.SharingDetailsDto(shareId) }
)
