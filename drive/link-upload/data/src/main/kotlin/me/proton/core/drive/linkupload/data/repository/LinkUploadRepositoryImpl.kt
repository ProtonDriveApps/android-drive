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
package me.proton.core.drive.linkupload.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.MediaResolution
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.iterator
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkupload.data.db.LinkUploadDatabase
import me.proton.core.drive.linkupload.data.db.entity.UploadBulkUriStringEntity
import me.proton.core.drive.linkupload.data.extension.toLinkUploadEntity
import me.proton.core.drive.linkupload.data.extension.toUploadBlock
import me.proton.core.drive.linkupload.data.extension.toUploadBlockEntity
import me.proton.core.drive.linkupload.data.extension.toUploadBulk
import me.proton.core.drive.linkupload.data.extension.toUploadBulkEntity
import me.proton.core.drive.linkupload.data.extension.toUploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadDigests
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.factory.UploadBlockFactory
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.util.kotlin.serialize
import javax.inject.Inject

class LinkUploadRepositoryImpl @Inject constructor(
    private val db: LinkUploadDatabase,
    private val uploadBlockFactory: UploadBlockFactory,
) : LinkUploadRepository {

    override suspend fun insertUploadFileLink(uploadFileLink: UploadFileLink): UploadFileLink =
        uploadFileLink.copy(
            id = db.linkUploadDao.insert(uploadFileLink.toLinkUploadEntity())
        )

    override suspend fun insertUploadFileLinks(uploadFileLinks: List<UploadFileLink>): List<UploadFileLink> =
        Pair(
            uploadFileLinks,
            db.linkUploadDao.insertAll(uploadFileLinks.map { uploadFileLink -> uploadFileLink.toLinkUploadEntity() })
        ).iterator().asSequence().map { (uploadFileLink, id) ->
            uploadFileLink.copy(id = id)
        }.toList()

    override suspend fun getUploadFileLink(uploadFileLinkId: Long): UploadFileLink? =
        db.linkUploadDao.get(uploadFileLinkId)?.toUploadFileLink()

    override suspend fun getUploadFileLink(fileId: FileId): UploadFileLink? =
        db.linkUploadDao.get(fileId.userId, fileId.shareId.id, fileId.id)?.toUploadFileLink()

    override fun getUploadFileLinkFlow(uploadFileLinkId: Long): Flow<UploadFileLink?> =
        db.linkUploadDao.getDistinctFlow(uploadFileLinkId).map { linkUploadEntity -> linkUploadEntity?.toUploadFileLink() }

    override fun getUploadFileLinks(userId: UserId): Flow<List<UploadFileLink>> =
        db.linkUploadDao.getAllFlow(userId)
            .map { linkUploadEntities ->
                linkUploadEntities.map { linkUploadEntity ->
                    linkUploadEntity.toUploadFileLink()
                }
            }

    override fun getUploadFileLinks(userId: UserId, parentId: FolderId): Flow<List<UploadFileLink>> =
        db.linkUploadDao.getAllFlow(userId, parentId.id)
            .map { linkUploadEntities ->
                linkUploadEntities.map { linkUploadEntity ->
                    linkUploadEntity.toUploadFileLink()
                }
            }

    override suspend fun updateUploadFileLink(uploadFileLink: UploadFileLink) =
        db.linkUploadDao.insertOrUpdate(uploadFileLink.toLinkUploadEntity())

    override suspend fun updateUploadFileLinkUploadState(uploadFileLinkId: Long, uploadState: UploadState) =
        db.linkUploadDao.updateUploadState(uploadFileLinkId, uploadState)

    override suspend fun updateUploadFileLinkFileInfo(
        uploadFileLinkId: Long,
        fileId: FileId,
        revisionId: String,
        name: String,
        nodeKey: String,
        nodePassphrase: String,
        nodePassphraseSignature: String,
        contentKeyPacket: String,
        contentKeyPacketSignature: String,
    ) =
        db.linkUploadDao.updateLinkIdAndRevisionId(
            id = uploadFileLinkId,
            linkId = fileId.id,
            revisionId = revisionId,
            name = name,
            nodeKey = nodeKey,
            nodePassphrase = nodePassphrase,
            nodePassphraseSignature = nodePassphraseSignature,
            contentKeyPacket = contentKeyPacket,
            contentKeyPacketSignature = contentKeyPacketSignature,
        )

    override suspend fun updateUploadFileLinkManifestSignature(uploadFileLinkId: Long, manifestSignature: String) =
        db.linkUploadDao.updateManifestSignature(uploadFileLinkId, manifestSignature)

    override suspend fun updateUploadFileLinkName(uploadFileLinkId: Long, name: String) =
        db.linkUploadDao.updateName(uploadFileLinkId, name)

    override suspend fun updateUploadFileLinkUri(uploadFileLinkId: Long, uriString: String) =
        db.linkUploadDao.updateUri(uploadFileLinkId, uriString)

    override suspend fun updateUploadFileLinkUri(
        uploadFileLinkId: Long,
        uriString: String,
        shouldDeleteSourceUri: Boolean,
    ) =
        db.linkUploadDao.updateUri(uploadFileLinkId, uriString, shouldDeleteSourceUri)

    override suspend fun updateUploadFileLinkSize(uploadFileLinkId: Long, size: Bytes) =
        db.linkUploadDao.updateSize(uploadFileLinkId, size.value)

    override suspend fun updateUploadFileLinkLastModified(uploadFileLinkId: Long, lastModified: TimestampMs?) =
        db.linkUploadDao.updateLastModified(uploadFileLinkId, lastModified?.value)

    override suspend fun updateUploadFileLinkMediaResolution(uploadFileLinkId: Long, mediaResolution: MediaResolution) =
        db.linkUploadDao.updateMediaResolution(
            id = uploadFileLinkId,
            mediaResolutionWidth = mediaResolution.width,
            mediaResolutionHeight = mediaResolution.height,
        )
    override suspend fun updateUploadFileLinkDigests(uploadFileLinkId: Long, digests: UploadDigests) =
        db.linkUploadDao.updateDigests(
            id = uploadFileLinkId,
            digests = digests.values.serialize()
        )

    override suspend fun removeUploadFileLink(uploadFileLinkId: Long) =
        db.linkUploadDao.delete(uploadFileLinkId)

    override suspend fun insertUploadBlocks(uploadFileLink: UploadFileLink, uploadBlocks: List<UploadBlock>) =
        db.inTransaction {
            db.uploadBlockDao.delete(uploadFileLink.id)
            db.uploadBlockDao.insertOrIgnore(
                *uploadBlocks.map { uploadBlock ->
                    uploadBlock.toUploadBlockEntity(
                        uploadFileLinkId = uploadFileLink.id
                    )
                }.toTypedArray()
            )
        }

    override suspend fun getUploadBlock(
        uploadFileLinkId: Long,
        uploadBlockIndex: Long
    ): UploadBlock? =
        db.uploadBlockDao.get(
            uploadLinkId = uploadFileLinkId,
            index = uploadBlockIndex,
        )?.toUploadBlock(uploadBlockFactory)

    override suspend fun getUploadBlocks(uploadFileLink: UploadFileLink): List<UploadBlock> =
        db.uploadBlockDao.get(uploadFileLink.id)
            .map { uploadBlockEntity -> uploadBlockEntity.toUploadBlock(uploadBlockFactory) }

    override suspend fun updateUploadBlock(uploadFileLink: UploadFileLink, uploadBlock: UploadBlock) =
        db.uploadBlockDao.insertOrUpdate(
            uploadBlock.toUploadBlockEntity(
                uploadFileLinkId = uploadFileLink.id,
            )
        )

    override suspend fun updateUploadBlockToken(
        uploadFileLinkId: Long,
        uploadBlockIndex: Long,
        token: String
    ) =
        db.uploadBlockDao.updateToken(
            uploadLinkId = uploadFileLinkId,
            index = uploadBlockIndex,
            token = token,
        )

    override suspend fun removeUploadBlocks(uploadFileLink: UploadFileLink) =
        db.uploadBlockDao.delete(uploadFileLink.id)

    override suspend fun insertUploadBulk(uploadBulk: UploadBulk): UploadBulk = db.inTransaction {
        val uploadBulkId = db.uploadBulkDao.insert(uploadBulk.toUploadBulkEntity())
        db.uploadBulkDao.insert(
            uploadBulk.uriStrings.map { uriString -> UploadBulkUriStringEntity(uploadBulkId, uriString) }
        )
        uploadBulk.copy(id = uploadBulkId)
    }

    override suspend fun removeUploadBulk(uploadBulkId: Long): UploadBulk = db.inTransaction {
        val uploadBulkWithUri = db.uploadBulkDao.get(uploadBulkId)
        db.uploadBulkDao.delete(uploadBulkWithUri.uploadBulkEntity)
        uploadBulkWithUri.toUploadBulk()
    }
}
