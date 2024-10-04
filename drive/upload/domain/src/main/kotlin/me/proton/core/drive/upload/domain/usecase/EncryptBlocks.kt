/*
 * Copyright (c) 2024 Proton AG.
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.size
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.upload.EncryptUploadBlocks
import me.proton.core.drive.crypto.domain.usecase.upload.EncryptUploadThumbnail
import me.proton.core.drive.crypto.domain.usecase.upload.ManifestSignature
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.file.base.domain.entity.ThumbnailType
import me.proton.core.drive.file.base.domain.extension.fileName
import me.proton.core.drive.file.base.domain.extension.index
import me.proton.core.drive.file.base.domain.extension.sha256
import me.proton.core.drive.file.base.domain.extension.toBlockType
import me.proton.core.drive.key.domain.entity.ContentKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.BuildContentKey
import me.proton.core.drive.key.domain.usecase.BuildNodeKey
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.factory.UploadBlockFactory
import me.proton.core.drive.linkupload.domain.usecase.GetUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.RemoveRawUploadBlock
import me.proton.core.drive.linkupload.domain.usecase.UpdateManifestSignature
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.share.domain.usecase.GetSignatureAddress
import me.proton.core.drive.thumbnail.domain.usecase.CreateThumbnail
import me.proton.core.drive.upload.domain.extension.blockFile
import me.proton.core.drive.upload.domain.provider.FileProvider
import me.proton.core.util.kotlin.takeIfNotEmpty
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class EncryptBlocks @Inject constructor(
    private val updateUploadState: UpdateUploadState,
    private val getBlockFolder: GetBlockFolder,
    private val getRawBlocks: GetSplitRawBlocks,
    private val encryptUploadBlocks: EncryptUploadBlocks,
    private val removeRawBlock: RemoveRawUploadBlock,
    private val addUploadBlocks: AddUploadBlocks,
    private val uploadBlockFactory: UploadBlockFactory,
    private val getSignatureAddress: GetSignatureAddress,
    private val buildNodeKey: BuildNodeKey,
    private val getNodeKey: GetNodeKey,
    private val buildContentKey: BuildContentKey,
    private val getAddressKeys: GetAddressKeys,
    private val getThumbnail: CreateThumbnail,
    private val encryptUploadThumbnail: EncryptUploadThumbnail,
    private val fileProvider: FileProvider,
    private val updateManifestSignature: UpdateManifestSignature,
    private val manifestSignature: ManifestSignature,
    private val getUploadBlocks: GetUploadBlocks,
) {
    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        uriString: String,
        outputFileSuffix: String? = null,
        shouldDeleteSource: Boolean = false,
        includePhotoThumbnail: Boolean = false,
        isCancelled: () -> Boolean = { false },
        coroutineContext: CoroutineContext = Dispatchers.IO,
    ): Result<UploadFileLink> = coRunCatching(coroutineContext) {
        with (uploadFileLink) {
            updateUploadState(id, UploadState.ENCRYPTING_BLOCKS).getOrThrow()
            try {
                val blockFolder = getBlockFolder(userId, this).getOrThrow()
                val unencryptedBlocks = getRawBlocks(id, blockFolder).getOrThrow()
                val signatureAddress = getSignatureAddress(uploadFileLink.shareId).getOrThrow()
                val uploadFileKey = buildFileKey(signatureAddress)
                val uploadFileContentKey = buildContentKey(uploadFileKey)
                val addressKey = getAddressKeys(userId, signatureAddress)
                encryptUploadBlocks(
                    contentKey = uploadFileContentKey,
                    encryptKey = uploadFileKey,
                    signKey = addressKey,
                    input = unencryptedBlocks.map { (_, file) -> file },
                    output = unencryptedBlocks.map { (index, file) -> file.blockFile(index + 1L, outputFileSuffix) },
                    coroutineContext = coroutineContext,
                ) { inputIndex, rawBlock, encryptedBlock, encSignature ->
                    val index = unencryptedBlocks[inputIndex].first
                    processEncryptedBlock(
                        uploadFileLinkId = id,
                        uploadBlock = uploadBlockFactory.create(
                            index = index + 1L,
                            block = encryptedBlock,
                            hashSha256 = encryptedBlock.sha256,
                            encSignature = encSignature,
                            rawSize = rawBlock.size,
                            size = encryptedBlock.size,
                            token = "",
                            type = Block.Type.FILE,
                            verifierToken = null,
                        ),
                        index = index,
                        unencryptedBlock = rawBlock,
                        isCancelled = isCancelled,
                    )
                }
                encryptThumbnails(
                    contentKey = uploadFileContentKey,
                    addressKey = addressKey,
                    uriString = uriString,
                    includePhotoThumbnail = includePhotoThumbnail,
                    coroutineContext = coroutineContext,
                )
                val sourceFile = fileProvider.getFile(uriString)
                if (shouldDeleteSource && sourceFile.exists()) sourceFile.delete()
                val uploadBlocks = getUploadBlocks(this, coroutineContext).getOrThrow()
                require(uploadBlocks.isNotEmpty()) { "Upload blocks should not be empty" }
                val emptyBlocks = uploadBlocks.filter { block -> block.file.length() == 0L }
                require(emptyBlocks.isEmpty()) {
                    "Encrypt should not generate empty file:" +
                            "\n${emptyBlocks.size}/${uploadBlocks.size}" +
                            "\n${emptyBlocks.map { block -> block.file.name }}"
                }
                updateManifestSignature(
                    uploadFileLinkId = id,
                    manifestSignature = manifestSignature(
                        signKey = addressKey,
                        input = uploadBlocks,
                        coroutineContext = coroutineContext,
                    ).getOrThrow()
                ).getOrThrow()
            } finally {
                updateUploadState(id, UploadState.IDLE)
            }
        }
    }

    private suspend fun UploadFileLink.encryptThumbnails(
        contentKey: ContentKey,
        addressKey: Key,
        uriString: String,
        includePhotoThumbnail: Boolean,
        coroutineContext: CoroutineContext,
    ) {
        listOfNotNull(
            getThumbnailUploadBlock(
                contentKey = contentKey,
                signKey = addressKey,
                uriString = uriString,
                type = ThumbnailType.DEFAULT,
                coroutineContext = coroutineContext,
            ),
            takeIf { includePhotoThumbnail }?.let {
                getThumbnailUploadBlock(
                    contentKey = contentKey,
                    signKey = addressKey,
                    uriString = uriString,
                    type = ThumbnailType.PHOTO,
                    coroutineContext = coroutineContext,
                )
            }
        ).takeIfNotEmpty()?.let { thumbnailBlocks ->
            addUploadBlocks(id, thumbnailBlocks).getOrThrow()
        }
    }

    private suspend fun UploadFileLink.getThumbnailUploadBlock(
        contentKey: ContentKey,
        signKey: Key,
        uriString: String,
        type: ThumbnailType,
        coroutineContext: CoroutineContext,
    ): UploadBlock? =
        getThumbnail(
            uri = uriString,
            mimeType = mimeType,
            type = type,
            coroutineContext = coroutineContext,
        ).getOrThrow()?.let { thumbnail ->
            val encryptedUploadThumbnail = File(
                getBlockFolder(userId, this).getOrThrow(),
                type.fileName,
            ).apply { createNewFile() }
            encryptUploadThumbnail(
                contentKey = contentKey,
                signKey = signKey,
                input = thumbnail,
                output = encryptedUploadThumbnail,
                coroutineContext = coroutineContext,
            ).getOrThrow()
            uploadBlockFactory.create(
                index = type.index,
                block = encryptedUploadThumbnail,
                hashSha256 = encryptedUploadThumbnail.sha256,
                encSignature = "",
                rawSize = thumbnail.size.bytes,
                size = encryptedUploadThumbnail.size,
                token = "",
                type = type.toBlockType(),
                verifierToken = null,
            )
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

    private suspend fun processEncryptedBlock(
        uploadFileLinkId: Long,
        uploadBlock: UploadBlock,
        index: Long,
        unencryptedBlock: File,
        isCancelled: () -> Boolean = { false },
    ) = withContext(NonCancellable) {
        if (isCancelled()) {
            uploadBlock.file.delete()
            throw CancellationException(RuntimeException("Cancelled by caller"))
        }
        addUploadBlocks(uploadFileLinkId, listOf(uploadBlock)).getOrThrow()
        removeRawBlock(uploadFileLinkId, index).getOrThrow()
        unencryptedBlock.delete()
    }
}
