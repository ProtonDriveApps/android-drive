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

import me.proton.core.drive.base.domain.entity.CameraExifTags
import me.proton.core.drive.base.domain.entity.Location
import me.proton.core.drive.base.domain.entity.MediaResolution
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderContext
import me.proton.core.drive.linkupload.data.db.entity.LinkUploadEntity
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.data.extension.toVolumeType
import me.proton.drive.sdk.entity.LegacyNodeUid
import me.proton.core.util.kotlin.takeIfNotEmpty
import kotlin.time.Duration.Companion.seconds

fun LinkUploadEntity.toUploadFileLink() =
    UploadFileLink(
        id = id,
        parentFolderContext = FolderContext(
            userId = userId,
            nodeUid = LegacyNodeUid(volumeId = volumeId, linkId = parentId),
            volumeType = volumeType.toVolumeType(),
        ),
        shareId = ShareId(userId, shareId),
        linkId = linkId.takeIfNotEmpty()?.let { FileId(ShareId(userId, shareId), it) },
        name = name,
        mimeType = mimeType,
        state = state,
        size = size?.bytes,
        lastModified = lastModified?.let { TimestampMs(lastModified) },
        uriString = uri,
        shouldDeleteSourceUri = shouldDeleteSourceUri,
        mediaResolution = takeIf { mediaResolutionWidth != null && mediaResolutionHeight != null }?.let {
            MediaResolution(
                width = requireNotNull(mediaResolutionWidth),
                height = requireNotNull(mediaResolutionHeight),
            )
        },
        networkTypeProviderType = networkTypeProviderType,
        mediaDuration = mediaDuration?.seconds,
        fileCreationDateTime = cameraCreationDateTime?.let(::TimestampS),
        location = takeIf { latitude != null && longitude != null }?.let {
            Location(
                latitude = requireNotNull(latitude),
                longitude = requireNotNull(longitude),
            )
        },
        cameraExifTags = takeIf { cameraModel != null && cameraOrientation != null }?.let {
            CameraExifTags(
                model = requireNotNull(cameraModel),
                orientation = requireNotNull(cameraOrientation).toInt(),
                subjectArea = cameraSubjectArea,
            )
        },
        shouldAnnounceEvent = shouldAnnounceEvent,
        priority = priority,
        uploadCreationDateTime = uploadCreationDateTime?.let(::TimestampS),
        shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
        attempts = attempts,
    )
