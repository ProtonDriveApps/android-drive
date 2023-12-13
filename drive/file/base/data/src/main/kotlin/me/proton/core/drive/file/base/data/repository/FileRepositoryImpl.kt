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
package me.proton.core.drive.file.base.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.extension.toDataResult
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.file.base.data.api.FileApiDataSource
import me.proton.core.drive.file.base.data.api.response.GetThumbnailsUrlsResponse
import me.proton.core.drive.file.base.data.extension.mapResults
import me.proton.core.drive.file.base.data.extension.toFileInfo
import me.proton.core.drive.file.base.data.extension.toRevisionInfo
import me.proton.core.drive.file.base.domain.entity.FileInfo
import me.proton.core.drive.file.base.domain.entity.NewFileInfo
import me.proton.core.drive.file.base.domain.entity.PhotoAttributes
import me.proton.core.drive.file.base.domain.entity.Revision
import me.proton.core.drive.file.base.domain.entity.ThumbnailId
import me.proton.core.drive.file.base.domain.entity.ThumbnailUrl
import me.proton.core.drive.file.base.domain.repository.FileRepository
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import me.proton.core.network.domain.ApiException
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class FileRepositoryImpl @Inject constructor(
    private val api: FileApiDataSource,
) : FileRepository {

    override suspend fun createNewFile(
        shareId: ShareId,
        newFileInfo: NewFileInfo
    ): Result<FileInfo> = coRunCatching {
        api.createFile(shareId, newFileInfo).toFileInfo(shareId)
    }

    override suspend fun fetchRevision(
        fileId: FileId,
        revisionId: String,
        fromBlockIndex: Int,
        pageSize: Int,
    ): Revision =
        api.getRevision(fileId, revisionId, fromBlockIndex, pageSize).toRevisionInfo()

    override suspend fun updateRevision(
        fileId: FileId,
        revisionId: String,
        manifestSignature: String,
        signatureAddress: String,
        blockNumber: Long,
        xAttr: String,
        photoAttributes: PhotoAttributes?,
    ): Result<Unit> = coRunCatching {
        api.updateRevision(
            fileId = fileId,
            revisionId = revisionId,
            manifestSignature = manifestSignature,
            signatureAddress = signatureAddress,
            blockNumber = blockNumber,
            xAttr = xAttr,
            photoAttributes = photoAttributes,
        )
    }

    override suspend fun deleteRevision(fileId: FileId, revisionId: String): Result<Unit> =
        coRunCatching {
            api.deleteRevision(
                fileId = fileId,
                revisionId = revisionId,
            )
        }

    override suspend fun fetchThumbnailsUrls(
        userId: UserId,
        volumeId: VolumeId,
        thumbnailIds: Set<ThumbnailId>,
    ): Map<ThumbnailId, Result<ThumbnailUrl>> = associateResult(thumbnailIds) {
        api.getThumbnailsUrls(
            userId = userId,
            volumeId = volumeId,
            thumbnailIds = thumbnailIds.map { thumbnailId -> thumbnailId.id }
        )
    }

    override suspend fun getUrlInputStream(
        userId: UserId,
        url: String
    ): Result<InputStream> = coRunCatching {
        api.getFileStream(userId, url)
    }

    override suspend fun uploadFile(
        userId: UserId,
        uploadUrl: String,
        uploadFile: File,
        uploadingProgress: MutableStateFlow<Long>,
    ): Result<Unit> = coRunCatching {
        api.uploadFile(userId, uploadUrl, uploadFile, uploadingProgress)
    }

    private inline fun associateResult(
        thumbnailIds: Set<ThumbnailId>,
        block: () -> GetThumbnailsUrlsResponse,
    ): Map<ThumbnailId, Result<ThumbnailUrl>> =
        try {
            block().mapResults(thumbnailIds)
        } catch (e: ApiException) {
            val result = e.toDataResult().toResult()
            thumbnailIds.associateWith { result }
        }
}
