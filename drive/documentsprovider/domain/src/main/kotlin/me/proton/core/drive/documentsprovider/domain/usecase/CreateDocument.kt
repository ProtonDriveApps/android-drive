/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.documentsprovider.domain.usecase

import android.provider.DocumentsContract
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.log.logId
import me.proton.core.drive.documentsprovider.domain.entity.DocumentId
import me.proton.core.drive.folder.create.domain.usecase.CreateFolder
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.upload.domain.usecase.CreateUploadFile
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class CreateDocument @Inject constructor(
    private val withDriveLinkFolder: WithDriveLinkFolder,
    private val createFolder: CreateFolder,
    private val createUploadFile: CreateUploadFile,
) {

    suspend operator fun invoke(
        parentDocumentId: DocumentId,
        mimeType: String?,
        displayName: String?
    ) =
        withDriveLinkFolder(parentDocumentId) { userId, driveLink ->
            requireNotNull(displayName) { "displayName cannot be null" }
            requireNotNull(mimeType) { "mimeType cannot be null" }
            if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                val (_, folderId) = createFolder(driveLink.id, displayName)
                    .getOrThrow()
                CoreLogger.d(
                    LogTag.DOCUMENTS_PROVIDER,
                    "Folder created with id: ${folderId.id.logId()}"
                )
                DocumentId(userId, folderId)
            } else {
                val uploadFileLink = createUploadFile(
                    userId = userId,
                    volumeId = driveLink.volumeId,
                    parentId = driveLink.id,
                    name = displayName,
                    mimeType = mimeType,
                    networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
                    shouldAnnounceEvent = true,
                    cacheOption = CacheOption.ALL,
                    priority = UploadFileLink.USER_PRIORITY,
                    shouldBroadcastErrorMessage = true,
                ).getOrThrow()
                CoreLogger.d(
                    LogTag.DOCUMENTS_PROVIDER,
                    "File waiting for sdk upload with id: ${uploadFileLink.id}"
                )
                DocumentId(userId, uploadId = uploadFileLink.id.toString())
            }
        }
}
