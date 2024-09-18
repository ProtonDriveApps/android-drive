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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class CreateUploadFile @Inject constructor(
    private val linkUploadRepository: LinkUploadRepository,
    private val getUploadFileName: GetUploadFileName,
    private val getUploadFileMimeType: GetUploadFileMimeType,
    private val getUploadFileSize: GetUploadFileSize,
    private val getUploadFileLastModified: GetUploadFileLastModified,
    private val getUploadFileUriInfo: GetUploadFileUriInfo,
) {
    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        parentId: FolderId,
        name: String,
        mimeType: String,
        networkTypeProviderType: NetworkTypeProviderType,
        shouldAnnounceEvent: Boolean,
        cacheOption: CacheOption,
        priority: Long,
        shouldBroadcastErrorMessage: Boolean,
    ): Result<UploadFileLink> = coRunCatching {
        linkUploadRepository.insertUploadFileLink(
            UploadFileLink(
                userId = userId,
                volumeId = volumeId,
                shareId = parentId.shareId,
                parentLinkId = parentId,
                name = name,
                mimeType = mimeType,
                networkTypeProviderType = networkTypeProviderType,
                shouldAnnounceEvent = shouldAnnounceEvent,
                cacheOption = cacheOption,
                priority = priority,
                shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
            )
        )
    }

    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        parentId: FolderId,
        uriString: String,
        shouldDeleteSourceUri: Boolean,
        networkTypeProviderType: NetworkTypeProviderType,
        shouldAnnounceEvent: Boolean,
        cacheOption: CacheOption,
        priority: Long,
        shouldBroadcastErrorMessage: Boolean,
        coroutineContext: CoroutineContext = Dispatchers.IO,
    ): Result<UploadFileLink> = coRunCatching(coroutineContext) {
        val uriInfo = getUploadFileUriInfo(uriString)
        val mimeType = getUploadFileMimeType(uriInfo)
        linkUploadRepository.insertUploadFileLink(
            UploadFileLink(
                userId = userId,
                volumeId = volumeId,
                shareId = parentId.shareId,
                parentLinkId = parentId,
                name = getUploadFileName(uriInfo),
                mimeType = mimeType,
                size = getUploadFileSize(uriInfo),
                lastModified = getUploadFileLastModified(uriInfo),
                uriString = uriString,
                shouldDeleteSourceUri = shouldDeleteSourceUri,
                networkTypeProviderType = networkTypeProviderType,
                shouldAnnounceEvent = shouldAnnounceEvent,
                cacheOption = cacheOption,
                priority = priority,
                shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
            )
        )
    }

    suspend operator fun invoke(
        userId: UserId,
        volumeId: VolumeId,
        parentId: FolderId,
        uploadFileDescriptions: List<UploadFileDescription>,
        shouldDeleteSourceUri: Boolean,
        networkTypeProviderType: NetworkTypeProviderType,
        shouldAnnounceEvent: Boolean,
        cacheOption: CacheOption,
        priority: Long,
        shouldBroadcastErrorMessage: Boolean,
    ): Result<List<UploadFileLink>> = coRunCatching {
        linkUploadRepository.insertUploadFileLinks(
            uploadFileDescriptions.map { description ->
                val uriString = description.uri
                val uriInfo = takeIf { description.properties == null }?.let { getUploadFileUriInfo(uriString) }
                val mimeType = getUploadFileMimeType(description, uriInfo)
                UploadFileLink(
                    userId = userId,
                    volumeId = volumeId,
                    shareId = parentId.shareId,
                    parentLinkId = parentId,
                    name = getUploadFileName(description, uriInfo),
                    mimeType = mimeType,
                    size = getUploadFileSize(description, uriInfo),
                    lastModified = getUploadFileLastModified(description, uriInfo),
                    uriString = uriString,
                    shouldDeleteSourceUri = shouldDeleteSourceUri,
                    networkTypeProviderType = networkTypeProviderType,
                    shouldAnnounceEvent = shouldAnnounceEvent,
                    cacheOption = cacheOption,
                    priority = priority,
                    shouldBroadcastErrorMessage = shouldBroadcastErrorMessage,
                )
            }
        )
    }
}
