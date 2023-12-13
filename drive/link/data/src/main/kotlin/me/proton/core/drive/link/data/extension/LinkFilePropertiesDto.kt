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
package me.proton.core.drive.link.data.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.data.api.entity.LinkFilePropertiesDto
import me.proton.core.drive.link.data.api.entity.LinkThumbnailDto
import me.proton.core.drive.link.data.db.entity.LinkFilePropertiesEntity

fun LinkFilePropertiesDto.toLinkFilePropertiesEntity(userId: UserId, shareId: String, id: String) =
    LinkFilePropertiesEntity(
        userId = userId,
        shareId = shareId,
        linkId = id,
        activeRevisionId = activeRevision.id,
        hasThumbnail = activeRevision.thumbnail == 1L,
        contentKeyPacket = contentKeyPacket,
        contentKeyPacketSignature = contentKeyPacketSignature,
        activeRevisionSignatureAddress = activeRevision.signatureAddress,
        photoCaptureTime = activeRevision.photo?.captureTime,
        photoContentHash = activeRevision.photo?.contentHash,
        mainPhotoLinkId = activeRevision.photo?.mainPhotoLinkId,
        defaultThumbnailId = activeRevision
            .thumbnails
            .firstOrNull { linkThumbnailDto -> linkThumbnailDto.type == LinkThumbnailDto.TYPE_DEFAULT }
            ?.thumbnailId,
        photoThumbnailId = activeRevision
            .thumbnails
            .firstOrNull { linkThumbnailDto -> linkThumbnailDto.type == LinkThumbnailDto.TYPE_PHOTO }
            ?.thumbnailId,
    )
