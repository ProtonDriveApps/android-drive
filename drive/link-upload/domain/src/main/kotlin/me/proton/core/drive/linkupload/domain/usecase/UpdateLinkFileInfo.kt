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
package me.proton.core.drive.linkupload.domain.usecase

import me.proton.core.drive.file.base.domain.entity.FileInfo
import me.proton.core.drive.file.base.domain.entity.NewFileInfo
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import javax.inject.Inject

class UpdateLinkFileInfo @Inject constructor(
    private val linkUploadRepository: LinkUploadRepository,
    private val getUploadFileLinkAfterOperation: GetUploadFileLinkAfterOperation,
) {
    suspend operator fun invoke(
        uploadFileLinkId: Long,
        newFileInfo: NewFileInfo,
        fileInfo: FileInfo,
    ): Result<UploadFileLink> = getUploadFileLinkAfterOperation(uploadFileLinkId) {
        linkUploadRepository.updateUploadFileLinkFileInfo(
            uploadFileLinkId = uploadFileLinkId,
            fileId = fileInfo.fileId,
            revisionId = fileInfo.draftRevisionId,
            name = newFileInfo.name,
            nodeKey = newFileInfo.nodeKey,
            nodePassphrase = newFileInfo.nodePassphrase,
            nodePassphraseSignature = newFileInfo.nodePassphraseSignature,
            contentKeyPacket = newFileInfo.contentKeyPacket,
            contentKeyPacketSignature = newFileInfo.contentKeyPacketSignature,
        )
    }
}
