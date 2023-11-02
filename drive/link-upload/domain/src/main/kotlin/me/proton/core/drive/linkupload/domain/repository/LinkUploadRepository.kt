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
package me.proton.core.drive.linkupload.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.MediaResolution
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadCount
import me.proton.core.drive.linkupload.domain.entity.UploadDigests
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState

interface LinkUploadRepository {

    suspend fun insertUploadFileLink(uploadFileLink: UploadFileLink): UploadFileLink

    suspend fun insertUploadFileLinks(uploadFileLinks: List<UploadFileLink>): List<UploadFileLink>

    suspend fun getUploadFileLink(uploadFileLinkId: Long): UploadFileLink?

    suspend fun getUploadFileLink(fileId: FileId): UploadFileLink?

    fun getUploadFileLinkFlow(uploadFileLinkId: Long): Flow<UploadFileLink?>

    fun getUploadFileLinks(userId: UserId): Flow<List<UploadFileLink>>

    fun getUploadFileLinks(userId: UserId, parentId: FolderId): Flow<List<UploadFileLink>>

    suspend fun getUploadFileLinksWithUri(
        userId: UserId,
        states: Set<UploadState>,
        count: Int,
    ): Flow<List<UploadFileLink>>

    fun getUploadFileLinksCount(userId: UserId): Flow<UploadCount>

    suspend fun updateUploadFileLink(uploadFileLink: UploadFileLink)

    suspend fun updateUploadFileLinkUploadState(uploadFileLinkId: Long, uploadState: UploadState)

    suspend fun updateUploadFileLinkUploadState(uploadFileLinkIds: Set<Long>, uploadState: UploadState)

    suspend fun updateUploadFileLinkFileInfo(
        uploadFileLinkId: Long,
        fileId: FileId,
        revisionId: String,
        name: String,
        nodeKey: String,
        nodePassphrase: String,
        nodePassphraseSignature: String,
        contentKeyPacket: String,
        contentKeyPacketSignature: String,
    )

    suspend fun updateUploadFileLinkManifestSignature(uploadFileLinkId: Long, manifestSignature: String)

    suspend fun updateUploadFileLinkName(uploadFileLinkId: Long, name: String)

    suspend fun updateUploadFileLinkUri(uploadFileLinkId: Long, uriString: String)

    suspend fun updateUploadFileLinkUri(uploadFileLinkId: Long, uriString: String, shouldDeleteSourceUri: Boolean)

    suspend fun updateUploadFileLinkSize(uploadFileLinkId: Long, size: Bytes)

    suspend fun updateUploadFileLinkLastModified(uploadFileLinkId: Long, lastModified: TimestampMs?)

    suspend fun updateUploadFileLinkMediaResolution(uploadFileLinkId: Long, mediaResolution: MediaResolution)

    suspend fun updateUploadFileLinkDigests(uploadFileLinkId: Long, digests: UploadDigests)

    suspend fun removeUploadFileLink(uploadFileLinkId: Long)

    suspend fun removeAllUploadFileLinks(userId: UserId, uploadState: UploadState)

    suspend fun insertUploadBlocks(uploadFileLink: UploadFileLink, uploadBlocks: List<UploadBlock>)

    suspend fun getUploadBlock(
        uploadFileLinkId: Long,
        uploadBlockIndex: Long,
    ): UploadBlock?

    suspend fun getUploadBlocks(uploadFileLink: UploadFileLink): List<UploadBlock>

    suspend fun updateUploadBlock(uploadFileLink: UploadFileLink, uploadBlock: UploadBlock)

    suspend fun updateUploadBlockToken(
        uploadFileLinkId: Long,
        uploadBlockIndex: Long,
        token: String,
    )

    suspend fun updateUploadBlockVerifierToken(
        uploadFileLinkId: Long,
        uploadBlockIndex: Long,
        verifierToken: ByteArray,
    )

    suspend fun removeUploadBlocks(uploadFileLink: UploadFileLink)

    suspend fun insertUploadBulk(uploadBulk: UploadBulk): UploadBulk

    suspend fun removeUploadBulk(uploadBulkId: Long): UploadBulk
}
