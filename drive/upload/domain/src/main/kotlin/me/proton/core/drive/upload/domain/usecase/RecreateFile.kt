/*
 * Copyright (c) 2023 Proton AG.
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

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.extension.updateRevisionInPath
import me.proton.core.drive.base.domain.usecase.GetSignatureAddress
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.CreateNewFileInfo
import me.proton.core.drive.file.base.domain.repository.FileRepository
import me.proton.core.drive.file.base.domain.usecase.MoveRevisionTo
import me.proton.core.drive.key.domain.entity.ContentKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.BuildContentKey
import me.proton.core.drive.key.domain.usecase.BuildNodeKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.factory.UploadBlockFactory
import me.proton.core.drive.linkupload.domain.usecase.GetUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.UpdateLinkFileInfo
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import javax.inject.Inject

class RecreateFile @Inject constructor(
    private val updateUploadState: UpdateUploadState,
    private val createNewFileInfo: CreateNewFileInfo,
    private val buildNodeKey: BuildNodeKey,
    private val buildContentKey: BuildContentKey,
    private val getLink: GetLink,
    private val getNodeKey: GetNodeKey,
    private val getSignatureAddress: GetSignatureAddress,
    private val fileRepository: FileRepository,
    private val updateLinkFileInfo: UpdateLinkFileInfo,
    private val moveRevisionTo: MoveRevisionTo,
    private val getUploadBlocks: GetUploadBlocks,
    private val addUploadBlocks: AddUploadBlocks,
    private val uploadBlockFactory: UploadBlockFactory,
) {
    suspend operator fun invoke(uploadFileLink: UploadFileLink): Result<UploadFileLink> = coRunCatching {
        with(uploadFileLink) {
            require(
                nodeKey.isNotEmpty() && nodePassphrase.isNotEmpty() && nodePassphraseSignature.isNotEmpty()
            ) {
                "Node key is required for recreate file"
            }
            val signatureAddress = getSignatureAddress(userId)
            val fileKey = buildFileKey(signatureAddress)
            val newFileInfo = createNewFileInfo(
                folder = getLink(parentLinkId).toResult().getOrThrow(),
                name = name,
                mimeType = mimeType,
                fileKey = fileKey,
                fileContentKey = buildContentKey(fileKey),
            ).getOrThrow()
            updateUploadState(id, UploadState.CREATING_NEW_FILE).getOrThrow()
            try {
                val fileInfo = fileRepository.createNewFile(
                    shareId = parentLinkId.shareId,
                    newFileInfo = newFileInfo,
                ).getOrThrow()
                moveRevisionTo(
                    userId = userId,
                    volumeId = volumeId,
                    fromRevisionId = draftRevisionId,
                    toRevisionId = fileInfo.draftRevisionId,
                )
                val uploadBlocks = getUploadBlocks(this).getOrThrow()
                    .map { uploadBlock ->
                        with(uploadBlock) {
                            uploadBlockFactory.create(
                                index = index,
                                url = url.updateRevisionInPath(volumeId.id, draftRevisionId, fileInfo.draftRevisionId),
                                hashSha256 = hashSha256,
                                encSignature = encSignature,
                                rawSize = rawSize,
                                size = size,
                                token = "",
                                type = type,
                                verifierToken = null,
                            )
                        }
                    }
                addUploadBlocks(this, uploadBlocks).getOrThrow()
                updateLinkFileInfo(
                    uploadFileLinkId = id,
                    newFileInfo = newFileInfo,
                    fileInfo = fileInfo,
                ).getOrThrow()
            } catch (e: Throwable) {
                updateUploadState(id, UploadState.IDLE)
                throw e
            }
        }
    }

    private suspend fun UploadFileLink.buildFileKey(signatureAddress: String): Key.Node =
        buildNodeKey(
            userId = userId,
            parentKey = getNodeKey(parentLinkId).getOrThrow(),
            uploadFileLink = this,
            signatureAddress = signatureAddress,
        ).getOrThrow()

    private suspend fun UploadFileLink.buildContentKey(uploadFileKey: Key.Node): ContentKey =
        buildContentKey(
            userId = userId,
            uploadFile = this,
            fileKey = uploadFileKey,
        ).getOrThrow()
}
