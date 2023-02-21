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
package me.proton.core.drive.file.base.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.file.base.data.api.entity.FileDto
import me.proton.core.drive.file.base.data.api.entity.RevisionDto
import me.proton.core.drive.file.base.data.api.request.UpdateRevisionRequest
import me.proton.core.drive.file.base.data.extension.createBlockFormData
import me.proton.core.drive.file.base.data.extension.toBlockTokenDto
import me.proton.core.drive.file.base.data.extension.toCreateFileRequest
import me.proton.core.drive.file.base.domain.entity.BlockTokenInfo
import me.proton.core.drive.file.base.domain.entity.NewFileInfo
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiException
import java.io.File

class FileApiDataSource(
    private val apiProvider: ApiProvider,
) {
    @Throws(ApiException::class)
    suspend fun createFile(
        shareId: ShareId,
        newFileInfo: NewFileInfo,
    ): FileDto =
        apiProvider.get<FileApi>(shareId.userId).invoke {
            createFile(
                shareId = shareId.id,
                request = newFileInfo.toCreateFileRequest(),
            )
        }.valueOrThrow.fileDto

    @Throws(ApiException::class)
    suspend fun getRevision(
        fileId: FileId,
        revisionId: String,
        fromBlockIndex: Int,
        pageSize: Int,
    ): RevisionDto =
        apiProvider.get<FileApi>(fileId.userId).invoke {
            getRevision(
                shareId = fileId.shareId.id,
                linkId = fileId.id,
                revisionId = revisionId,
                fromBlockIndex = fromBlockIndex,
                pageSize = pageSize,
            )
        }.valueOrThrow.revisionDto

    @Throws(ApiException::class)
    suspend fun updateRevision(
        fileId: FileId,
        revisionId: String,
        blockTokenInfos: List<BlockTokenInfo>,
        manifestSignature: String,
        signatureAddress: String,
        blockNumber: Long,
        state: Long,
        xAttr: String,
    ) =
        apiProvider.get<FileApi>(fileId.userId).invoke {
            updateRevision(
                shareId = fileId.shareId.id,
                linkId = fileId.id,
                revisionId = revisionId,
                request = UpdateRevisionRequest(
                    blockList = blockTokenInfos.map { blockTokenInfo -> blockTokenInfo.toBlockTokenDto() },
                    manifestSignature = manifestSignature,
                    signatureAddress = signatureAddress,
                    blockNumber = blockNumber,
                    state = state,
                    xAttr = xAttr,
                ),
            )
        }.valueOrThrow

    @Throws(ApiException::class)
    suspend fun getThumbnailUrl(
        fileId: FileId,
        revisionId: String,
    ) =
        apiProvider.get<FileApi>(fileId.userId).invoke {
            getThumbnailUrl(
                shareId = fileId.shareId.id,
                linkId = fileId.id,
                revisionId = revisionId,
            )
        }.valueOrThrow

    @Throws(ApiException::class, IllegalStateException::class)
    suspend fun getFileStream(
        userId: UserId,
        url: String,
    ) =
        apiProvider.get<FileApi>(userId).invoke {
            getFileStream(url)
        }.valueOrThrow.body()?.byteStream() ?: throw IllegalStateException("Response body is null")

    @Throws(ApiException::class)
    suspend fun uploadFile(
        userId: UserId,
        url: String,
        file: File,
        progress: MutableStateFlow<Long>,
    ) =
        apiProvider.get<FileApi>(userId).invoke(forceNoRetryOnConnectionErrors = true) {
            uploadFile(
                url = url,
                filePart = file.createBlockFormData(progress),
            )
        }.valueOrThrow
}
