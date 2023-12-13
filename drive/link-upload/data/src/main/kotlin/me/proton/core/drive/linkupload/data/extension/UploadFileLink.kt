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
package me.proton.core.drive.linkupload.data.extension

import me.proton.core.drive.linkupload.data.db.entity.LinkUploadEntity
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink

fun UploadFileLink.toLinkUploadEntity() =
    LinkUploadEntity(
        id = id,
        userId = userId,
        volumeId = volumeId.id,
        shareId = shareId.id,
        parentId = parentLinkId.id,
        linkId = linkId.orEmpty(),
        revisionId = draftRevisionId,
        name = name,
        mimeType = mimeType,
        nodeKey = nodeKey,
        nodePassphrase = nodePassphrase,
        nodePassphraseSignature = nodePassphraseSignature,
        contentKeyPacket = contentKeyPacket,
        contentKeyPacketSignature = contentKeyPacketSignature,
        manifestSignature = manifestSignature,
        state = state,
        size = size?.value,
        lastModified = lastModified?.value,
        uri = uriString,
        shouldDeleteSourceUri = shouldDeleteSourceUri,
        mediaResolutionWidth = mediaResolution?.width,
        mediaResolutionHeight = mediaResolution?.height,
        networkTypeProviderType = networkTypeProviderType,
        mediaDuration = mediaDuration?.inWholeSeconds,
        latitude = location?.latitude,
        longitude = location?.longitude,
        cameraCreationDateTime = fileCreationDateTime?.value,
        cameraModel = cameraExifTags?.model,
        cameraOrientation = cameraExifTags?.orientation?.toLong(),
        cameraSubjectArea = cameraExifTags?.subjectArea,
        shouldAnnounceEvent = shouldAnnounceEvent,
        cacheOption = cacheOption,
        priority = priority,
        uploadCreationDateTime = uploadCreationDateTime?.value,
        shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
    )
