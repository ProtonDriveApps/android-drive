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
package me.proton.core.drive.upload.domain.usecase

import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetAddressId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.block.domain.entity.UploadBlocksUrl
import me.proton.core.drive.block.domain.repository.BlockRepository
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.extension.isThumbnail
import me.proton.core.drive.linkupload.domain.extension.requireFileId
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import javax.inject.Inject

class GetBlocksUploadUrl @Inject constructor(
    private val getAddressId: GetAddressId,
    private val blockRepository: BlockRepository,
    private val linkUploadRepository: LinkUploadRepository,
    private val updateUploadState: UpdateUploadState,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(uploadFileLink: UploadFileLink): Result<UploadBlocksUrl> = coRunCatching {
        with(uploadFileLink) {
            updateUploadState(id, UploadState.GETTING_UPLOAD_LINKS).getOrThrow()
            try {
                linkUploadRepository
                    .getUploadBlocks(this)
                    .chunked(configurationProvider.apiBlockPageSize)
                    .map { uploadBlocks ->
                        val thumbnailBlock = uploadBlocks.firstOrNull { uploadBlock -> uploadBlock.isThumbnail }
                        val fileBlocks = uploadBlocks.filterNot { uploadBlock -> uploadBlock.isThumbnail }
                        blockRepository.getUploadBlocksUrl(
                            userId = userId,
                            addressId = getAddressId(userId),
                            fileId = uploadFileLink.requireFileId(),
                            revisionId = draftRevisionId,
                            uploadBlocks = fileBlocks,
                            uploadThumbnail = thumbnailBlock,
                        ).getOrThrow()
                    }.flatten()
            } catch (e: Throwable) {
                updateUploadState(id, UploadState.IDLE)
                throw e
            }
        }
    }

    suspend operator fun invoke(uploadFileLink: UploadFileLink, index: Long): Result<UploadBlocksUrl> = coRunCatching {
        val uploadBlocks = linkUploadRepository.getUploadBlocks(uploadFileLink)
        val fileBlocks = uploadBlocks.filter { uploadBlock -> uploadBlock.index == index }
        require(fileBlocks.size == 1)
        val (uploadBlock, uploadThumbnail) = if (fileBlocks.first().isThumbnail) {
            emptyList<UploadBlock>() to fileBlocks.first()
        } else {
            fileBlocks to null
        }
        with (uploadFileLink) {
            blockRepository.getUploadBlocksUrl(
                userId = userId,
                addressId = getAddressId(userId),
                fileId = uploadFileLink.requireFileId(),
                revisionId = draftRevisionId,
                uploadBlocks = uploadBlock,
                uploadThumbnail = uploadThumbnail,
            ).getOrThrow()
        }
    }

    private fun List<UploadBlocksUrl>.flatten(): UploadBlocksUrl =
        UploadBlocksUrl(
            blockLinks = map { uploadBlocksUrl -> uploadBlocksUrl.blockLinks }.flatten(),
            thumbnailLink = firstOrNull { uploadBlocksUrl -> uploadBlocksUrl.thumbnailLink != null }?.thumbnailLink,
        )
}
