/*
 * Copyright (c) 2022-2024 Proton AG.
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

import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.data.db.entity.UploadBulkWithUri
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId

fun UploadBulkWithUri.toUploadBulk() =
    UploadBulk(
        id = uploadBulkEntity.id,
        userId = uploadBulkEntity.userId,
        volumeId = VolumeId(uploadBulkEntity.volumeId),
        shareId = ShareId(uploadBulkEntity.userId, uploadBulkEntity.shareId),
        parentLinkId = FolderId(ShareId(uploadBulkEntity.userId, uploadBulkEntity.shareId), uploadBulkEntity.parentId),
        uploadFileDescriptions = uploadBulkUriStringEntity
            .sortedBy { entity -> entity.key }
            .map { entity -> entity.toUploadFileDescription() },
        shouldDeleteSourceUri = uploadBulkEntity.shouldDeleteSourceUri,
        networkTypeProviderType = uploadBulkEntity.networkTypeProviderType,
        shouldAnnounceEvent = uploadBulkEntity.shouldAnnounceEvent,
        cacheOption = uploadBulkEntity.cacheOption,
        priority = uploadBulkEntity.priority,
        shouldBroadcastErrorMessage = uploadBulkEntity.shouldBroadcastErrorMessage,
    )
