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

package me.proton.core.drive.upload.domain.usecase

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.toInstant
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.GetFileName
import me.proton.core.drive.link.domain.extension.nodeUid
import me.proton.core.drive.link.domain.extension.toSdkPhotoTag
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.GetPhotoTags
import me.proton.core.drive.linkupload.domain.usecase.UpdateSize
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadFileCreationTime
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.upload.domain.manager.UploadSdkManager
import me.proton.drive.sdk.entity.FileUploaderRequest
import me.proton.drive.sdk.entity.PhotosUploaderRequest
import java.time.Instant
import javax.inject.Inject

class CreateNewFileSdk @Inject constructor(
    private val uploadSdkManager: UploadSdkManager,
    private val updateUploadState: UpdateUploadState,
    private val getFileName: GetFileName,
    private val updateUploadFileCreationTime: UpdateUploadFileCreationTime,
    private val configurationProvider: ConfigurationProvider,
    private val getPhotoTags: GetPhotoTags,
    private val getShare: GetShare,
    private val photoAdditionalMetadata: PhotoAdditionalMetadata,
    private val getInputStreamSize: GetInputStreamSize,
    private val updateSize: UpdateSize,
) {

    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        uriString: String,
    ) = coRunCatching {
        coroutineScope {
            val id = uploadFileLink.id
            try {
                updateUploadState(id, UploadState.CREATING_NEW_FILE).getOrThrow()
                updateUploadFileCreationTime(id, TimestampS()).getOrThrow()
                val fileName = getFileName(
                    name = uploadFileLink.name,
                    folderId = uploadFileLink.parentLinkId,
                ).getOrThrow()
                val size = uploadFileLink.getOrUpdateSize(uriString).getOrThrow()
                val lastModified = uploadFileLink.lastModified?.toInstant()
                if (uploadFileLink.isPhoto()) {
                    uploadFileLink.enqueuePhoto(
                        name = fileName,
                        size = size,
                        lastModified = lastModified,
                    )
                } else {
                    uploadFileLink.enqueue(
                        name = fileName,
                        size = size,
                        lastModified = lastModified,
                    )
                }
            } finally {
                if (!isActive) {
                    withContext(NonCancellable) {
                        updateUploadState(id, UploadState.IDLE)
                    }
                }
            }
        }
    }

    private suspend fun UploadFileLink.enqueue(
        name: String,
        size: Bytes,
        lastModified: Instant?,
    ) = uploadSdkManager.enqueue(this@enqueue) { client ->
        // TODO implement enqueue without timeout and noWaiting = true
        withTimeout(configurationProvider.sdkQueueTimeout) {
            client.uploader(
                FileUploaderRequest(
                    parentFolderUid = parentLinkId.nodeUid(volumeId),
                    name = name,
                    mediaType = mimeType,
                    fileSize = size.value,
                    lastModificationTime = lastModified,
                    overrideExistingDraftByOtherClient = false,
                    noWaiting = false,
                )
            )
        }
    }

    private suspend fun UploadFileLink.enqueuePhoto(
        name: String,
        size: Bytes,
        lastModified: Instant?,
    ) = uploadSdkManager.enqueuePhoto(this@enqueuePhoto) { client ->
        val tags = getPhotoTags(this@enqueuePhoto.id).getOrThrow()
        // TODO implement enqueue without timeout and noWaiting = true
        withTimeout(configurationProvider.sdkQueueTimeout) {
            client.uploader(
                request = PhotosUploaderRequest(
                    name = name,
                    mediaType = mimeType,
                    fileSize = size.value,
                    lastModificationTime = lastModified,
                    captureTime = fileCreationDateTime?.toInstant(),
                    mainPhotoUid = null,
                    overrideExistingDraftByOtherClient = false,
                    additionalMetadata = photoAdditionalMetadata(this@enqueuePhoto),
                    tags = tags.map { photoTag -> photoTag.toSdkPhotoTag() },
                    noWaiting = false,
                ),
            )
        }
    }

    private suspend fun UploadFileLink.isPhoto(): Boolean {
        val share = getShare(shareId, flowOf(false)).toResult().getOrThrow()
        return share.type == Share.Type.PHOTO
    }

    private suspend fun UploadFileLink.getOrUpdateSize(
        uriString: String
    ): Result<Bytes> = coRunCatching {
        val uriSize = getInputStreamSize(uriString).getOrThrow()
        val uploadFileLinkSize = size
        if (uploadFileLinkSize != null && uploadFileLinkSize == uriSize) {
            return@coRunCatching uploadFileLinkSize
        }
        updateSize(id, uriSize).getOrThrow()
        uriSize
    }
}
