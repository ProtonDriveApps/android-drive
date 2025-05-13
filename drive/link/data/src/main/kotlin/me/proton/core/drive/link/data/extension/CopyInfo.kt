/*
 * Copyright (c) 2025 Proton AG.
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

import me.proton.core.drive.link.data.api.request.CopyLinkRequest
import me.proton.core.drive.link.data.api.request.CopyLinkRequest.Photos
import me.proton.core.drive.link.data.api.request.CopyLinkRequest.RelatedPhoto
import me.proton.core.drive.link.domain.entity.CopyInfo

fun CopyInfo.toCopyLinkRequest() =
    CopyLinkRequest(
        name = name,
        hash = hash,
        targetVolumeId = targetVolumeId,
        targetParentLinkId = targetParentLinkId,
        nodePassphrase = nodePassphrase,
        nodePassphraseSignature = nodePassphraseSignature,
        nameSignatureEmail = nameSignatureEmail,
        signatureEmail = signatureEmail,
        photos = (this as? CopyInfo.Photo)?.photos?.toCopyLinkRequestPhotos(),
)

fun CopyInfo.Photo.Photos.toCopyLinkRequestPhotos() =
    Photos(
        contentHash = contentHash,
        relatedPhotos = relatedPhotos.map { relatedPhoto ->
            relatedPhoto.toCopyLinkRequestRelatedPhoto()
        },
    )

fun CopyInfo.Photo.RelatedPhoto.toCopyLinkRequestRelatedPhoto() =
    RelatedPhoto(
        linkId = linkId,
        name = name,
        hash = hash,
        nodePassphrase = nodePassphrase,
        contentHash = contentHash,
    )
