/*
 * Copyright (c) 2021-2024 Proton AG.
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

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderContext
import me.proton.core.drive.link.domain.entity.NodeContext
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.Volume
import javax.inject.Inject

class CreateUploadFile @Inject constructor(
    private val linkUploadRepository: LinkUploadRepository,
    private val isUploadFileExist: IsUploadFileExist,
    private val getUploadFileName: GetUploadFileName,
    private val getUploadFileMimeType: GetUploadFileMimeType,
    private val getUploadFileSize: GetUploadFileSize,
    private val getUploadFileLastModified: GetUploadFileLastModified,
    private val getUploadFileUriInfo: GetUploadFileUriInfo,
) {
    suspend operator fun invoke(
        parentFolderContext: FolderContext,
        shareId: ShareId,
        name: String,
        mimeType: String,
        networkTypeProviderType: NetworkTypeProviderType,
        shouldAnnounceEvent: Boolean,
        priority: Long,
        shouldBroadcastErrorMessage: Boolean,
    ): Result<UploadFileLink> = coRunCatching {
        parentFolderContext.requireKnownVolumeType()
        linkUploadRepository.insertUploadFileLink(
            UploadFileLink(
                parentFolderContext = parentFolderContext,
                shareId = shareId,
                name = name,
                mimeType = mimeType,
                networkTypeProviderType = networkTypeProviderType,
                shouldAnnounceEvent = shouldAnnounceEvent,
                priority = priority,
                shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
            )
        )
    }

    suspend operator fun invoke(
        parentFolderContext: FolderContext,
        shareId: ShareId,
        uploadFileDescriptions: List<UploadFileDescription>,
        shouldDeleteSourceUri: Boolean,
        networkTypeProviderType: NetworkTypeProviderType,
        shouldAnnounceEvent: Boolean,
        priority: Long,
        shouldBroadcastErrorMessage: Boolean,
    ): Result<List<UploadFileLink>> = coRunCatching {
        parentFolderContext.requireKnownVolumeType()
        linkUploadRepository.insertUploadFileLinks(
            uploadFileDescriptions.filter { description ->
                isUploadFileExist(description)
            }.map { description ->
                val uriString = description.uri
                val uriInfo = takeIf { description.properties == null }?.let { getUploadFileUriInfo(uriString) }
                val mimeType = getUploadFileMimeType(description, uriInfo)
                UploadFileLink(
                    parentFolderContext = parentFolderContext,
                    shareId = shareId,
                    name = getUploadFileName(description, uriInfo),
                    mimeType = mimeType,
                    size = getUploadFileSize(description, uriInfo),
                    lastModified = getUploadFileLastModified(description, uriInfo),
                    uriString = uriString,
                    shouldDeleteSourceUri = shouldDeleteSourceUri,
                    networkTypeProviderType = networkTypeProviderType,
                    shouldAnnounceEvent = shouldAnnounceEvent,
                    priority = priority,
                    shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
                )
            }
        )
    }


    private fun NodeContext.requireKnownVolumeType() =
        require(volumeType != Volume.Type.UNKNOWN) {
            "Cannot queue an upload for an unknown volume type"
        }
}
