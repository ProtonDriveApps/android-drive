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
package me.proton.core.drive.block.data.api

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.block.data.api.request.BlockUploadRequest
import me.proton.core.drive.block.data.api.response.BlockUploadResponse
import me.proton.core.drive.block.data.extension.toUploadBlockDto
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException
import me.proton.core.user.domain.entity.AddressId

class BlockApiDataSource(private val apiProvider: ApiProvider) {

    @Throws(ApiException::class)
    suspend fun uploadBlock(
        userId: UserId,
        addressId: AddressId,
        fileId: FileId,
        revisionId: String,
        uploadBlocks: List<UploadBlock>,
        uploadThumbnail: UploadBlock?,
    ): BlockUploadResponse =
        apiProvider.get<BlockApi>(userId).invoke {
            blockUploadInfo(
                BlockUploadRequest(
                    blockList = uploadBlocks.map { uploadBlock -> uploadBlock.toUploadBlockDto() },
                    addressId = addressId.id,
                    shareId = fileId.shareId.id,
                    linkId = fileId.id,
                    revisionId = revisionId,
                    thumbnail = if (uploadThumbnail != null) 1 else 0,
                    thumbnailHash = uploadThumbnail?.hashSha256,
                    thumbnailSize = uploadThumbnail?.size?.value,
                )
            )
        }.valueOrThrow
}
