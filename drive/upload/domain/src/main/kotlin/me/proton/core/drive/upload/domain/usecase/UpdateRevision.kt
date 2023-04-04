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

import me.proton.core.drive.base.domain.usecase.GetSignatureAddress
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.link.EncryptAndSignXAttr
import me.proton.core.drive.file.base.domain.usecase.CreateXAttr
import me.proton.core.drive.file.base.domain.usecase.UpdateRevision
import me.proton.core.drive.file.base.domain.usecase.UpdateRevision.Companion.STATE_ACTIVE
import me.proton.core.drive.key.domain.usecase.BuildNodeKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.extension.isThumbnail
import me.proton.core.drive.linkupload.domain.extension.requireFileId
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.upload.domain.extension.toBlockTokenInfo
import java.util.Date
import javax.inject.Inject

class UpdateRevision @Inject constructor(
    private val updateRevision: UpdateRevision,
    private val getSignatureAddress: GetSignatureAddress,
    private val linkUploadRepository: LinkUploadRepository,
    private val updateUploadState: UpdateUploadState,
    private val createXAttr: CreateXAttr,
    private val encryptAndSignXAttr: EncryptAndSignXAttr,
    private val buildNodeKey: BuildNodeKey,
    private val getNodeKey: GetNodeKey,
) {
    suspend operator fun invoke(uploadFileLink: UploadFileLink): Result<UploadFileLink> = coRunCatching {
        with(uploadFileLink) {
            updateUploadState(id, UploadState.UPDATING_REVISION).getOrThrow()
            try {
                val fileId = uploadFileLink.requireFileId()
                val folderKey = getNodeKey(parentLinkId).getOrThrow()
                val signatureAddress = getSignatureAddress(userId)
                val uploadFileKey = buildNodeKey(userId, folderKey, this, signatureAddress).getOrThrow()
                val uploadBlocks = linkUploadRepository.getUploadBlocks(this)
                updateRevision(
                    fileId = fileId,
                    revisionId = draftRevisionId,
                    blockTokenInfos = uploadBlocks.map { uploadBlock -> uploadBlock.toBlockTokenInfo() },
                    manifestSignature = manifestSignature,
                    signatureAddress = signatureAddress,
                    blockNumber = uploadBlocks.size.toLong(),
                    state = STATE_ACTIVE,
                    xAttr = encryptAndSignXAttr(
                        userId = userId,
                        encryptKey = uploadFileKey,
                        signatureAddress = signatureAddress,
                        xAttr = createXAttr(
                            modificationTime = Date(lastModified?.value ?: System.currentTimeMillis()),
                            size = requireNotNull(size),
                            blockSizes = uploadBlocks
                                .filterNot { uploadBlock -> uploadBlock.isThumbnail }
                                .map { uploadBlock -> uploadBlock.rawSize },
                            mediaResolution = uploadFileLink.mediaResolution,
                            digests = uploadFileLink.digests.values,
                        ),
                    ).getOrThrow()
                ).getOrThrow()
                uploadFileLink
            } catch (e: Throwable) {
                updateUploadState(id, UploadState.IDLE)
                throw e
            }
        }
    }
}
