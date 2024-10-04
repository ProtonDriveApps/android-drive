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
package me.proton.core.drive.upload.domain.usecase

import kotlinx.coroutines.Dispatchers
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.size
import me.proton.core.drive.base.domain.extension.toHex
import me.proton.core.drive.base.domain.log.LogTag.UploadTag.logTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
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
import me.proton.core.drive.linkupload.domain.entity.UploadDigests
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.factory.UploadBlockFactory
import me.proton.core.drive.linkupload.domain.usecase.UpdateDigests
import me.proton.core.drive.linkupload.domain.usecase.UpdateManifestSignature
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.share.domain.usecase.GetSignatureAddress
import me.proton.core.drive.thumbnail.domain.usecase.CreateThumbnail
import me.proton.core.drive.upload.domain.extension.blockFile
import me.proton.core.drive.upload.domain.extension.injectMessageDigests
import me.proton.core.drive.upload.domain.extension.saveToBlocks
import me.proton.core.drive.upload.domain.provider.FileProvider
import me.proton.core.drive.upload.domain.resolver.UriResolver
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@Suppress("LongParameterList")
class SplitFileToBlocksAndEncrypt @Inject constructor(
    private val getNodeKey: GetNodeKey,
    private val buildNodeKey: BuildNodeKey,
    private val buildContentKey: BuildContentKey,
    private val encryptUploadBlocks: EncryptUploadBlocks,
    private val encryptUploadThumbnail: EncryptUploadThumbnail,
    private val updateUploadState: UpdateUploadState,
    private val uriResolver: UriResolver,
    private val getBlockFolder: GetBlockFolder,
    private val configurationProvider: ConfigurationProvider,
    private val uploadBlockFactory: UploadBlockFactory,
    private val getThumbnail: CreateThumbnail,
    private val addUploadBlocks: AddUploadBlocks,
    private val manifestSignature: ManifestSignature,
    private val getAddressKeys: GetAddressKeys,
    private val updateManifestSignature: UpdateManifestSignature,
    private val fileProvider: FileProvider,
    private val getSignatureAddress: GetSignatureAddress,
    private val updateDigests: UpdateDigests,
) {
    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        uriString: String,
        shouldDeleteSource: Boolean = false,
        includePhotoThumbnail: Boolean = false,
        coroutineContext: CoroutineContext = Dispatchers.IO,
    ): Result<UploadFileLink> = coRunCatching(coroutineContext) {
        val (unencryptedBlocks, digests) = uploadFileLink.splitUriToBlocks(
            uriString = uriString
        )
        updateDigests(
            uploadFileLinkId = uploadFileLink.id,
            digests = digests
        ).getOrThrow()
        encryptBlocks(
            uploadFileLink = uploadFileLink,
            unencryptedBlocks = unencryptedBlocks,
            uriString = uriString,
            includePhotoThumbnail = includePhotoThumbnail,
            coroutineContext = coroutineContext,
        ).getOrThrow().also {
            if (shouldDeleteSource) fileProvider.getFile(uriString).delete()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun encryptBlocks(
        uploadFileLink: UploadFileLink,
        unencryptedBlocks: List<File>,
        uriString: String,
        includePhotoThumbnail: Boolean = false,
        coroutineContext: CoroutineContext,
    ): Result<UploadFileLink> = coRunCatching(coroutineContext) {
        with(uploadFileLink) {
            updateUploadState(id, UploadState.ENCRYPTING_BLOCKS).getOrThrow()
            try {
                val signatureAddress = getSignatureAddress(uploadFileLink.shareId).getOrThrow()
                val uploadFileKey = buildFileKey(signatureAddress)
                val uploadFileContentKey = buildContentKey(uploadFileKey)
                val addressKey = getAddressKeys(userId, signatureAddress)
                val uploadBlocks = unencryptedBlocks
                    .encryptBlocksAndDeleteSourceFiles(
                        uploadFileLink = uploadFileLink,
                        uploadFileContentKey = uploadFileContentKey,
                        encryptKey = uploadFileKey,
                        signKey = addressKey,
                        coroutineContext = coroutineContext,
                    ) +
                        listOfNotNull(
                            getThumbnailUploadBlock(
                                uploadFileContentKey = uploadFileContentKey,
                                signKey = addressKey,
                                uriString = uriString,
                                type = ThumbnailType.DEFAULT,
                                coroutineContext = coroutineContext,
                            ),
                            takeIf { includePhotoThumbnail }?.let {
                                getThumbnailUploadBlock(
                                    uploadFileContentKey = uploadFileContentKey,
                                    signKey = addressKey,
                                    uriString = uriString,
                                    type = ThumbnailType.PHOTO,
                                    coroutineContext = coroutineContext,
                                )
                            }
                        )
                require(uploadBlocks.isNotEmpty()) { "Upload blocks should not be empty" }
                val emptyBlocks = uploadBlocks.filter { block -> block.file.length() == 0L }
                require(emptyBlocks.isEmpty()) {
                    "Encrypt should not generate empty file:" +
                            "\n${emptyBlocks.size}/${uploadBlocks.size}" +
                            "\n${emptyBlocks.map { block -> block.file.name }}"
                }
                addUploadBlocks(uploadBlocks)
                updateManifestSignature(
                    uploadFileLinkId = id,
                    manifestSignature = manifestSignature(
                        signKey = addressKey,
                        input = uploadBlocks,
                        coroutineContext = coroutineContext,
                    ).getOrThrow()
                ).getOrThrow()
            } catch (e: Throwable) {
                updateUploadState(id, UploadState.IDLE)
                throw e
            }
        }
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

    private suspend fun UploadFileLink.splitUriToBlocks(
        uriString: String,
    ): Pair<List<File>, UploadDigests> =
        uriResolver.useInputStream(uriString) { inputStream ->
            val (digestsInputStream, messageDigests) = inputStream.injectMessageDigests(
                algorithms = configurationProvider.digestAlgorithms,
            )
            val files = digestsInputStream.saveToBlocks(
                destinationFolder = getBlockFolder(userId, this).getOrThrow(),
                blockMaxSize = configurationProvider.blockMaxSize,
            )
            val emptyFiles = files.filter { file -> file.length() == 0L }
            require(emptyFiles.isEmpty()) {
                "Split should not generate empty file: " +
                        "\n${emptyFiles.size}/${files.size}" +
                        "\n${emptyFiles.map { file -> file.name }}"
            }
            val uploadDigests = messageDigests
                .associate { messageDigest ->
                    messageDigest.algorithm to messageDigest.digest().toHex()
                }.let(::UploadDigests)
            files to uploadDigests
        } ?: (emptyList<File>() to UploadDigests())

    private suspend fun List<File>.encryptBlocksAndDeleteSourceFiles(
        uploadFileLink: UploadFileLink,
        uploadFileContentKey: ContentKey,
        encryptKey: Key.Node,
        signKey: Key,
        coroutineContext: CoroutineContext,
    ): List<UploadBlock> =
        encryptUploadBlocks(
            contentKey = uploadFileContentKey,
            encryptKey = encryptKey,
            signKey = signKey,
            input = this,
            output = mapIndexed { index, file -> file.blockFile(index + 1L) },
            coroutineContext = coroutineContext,
        ) { index, rawBlock, encryptedBlock, encSignature ->
            CoreLogger.d(
                tag = uploadFileLink.id.logTag(),
                message = """
                    Block index $index, plain file size ${rawBlock.size}, encrypted file size ${encryptedBlock.size}
                """.trimIndent(),
            )
            uploadBlockFactory.create(
                index = index + 1L,
                block = encryptedBlock,
                hashSha256 = encryptedBlock.sha256,
                encSignature = encSignature,
                rawSize = rawBlock.size,
                size = encryptedBlock.size,
                token = "",
                type = Block.Type.FILE,
                verifierToken = null,
            ).also { rawBlock.delete() }
        }.getOrThrow()

    private suspend fun UploadFileLink.getThumbnailUploadBlock(
        uploadFileContentKey: ContentKey,
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
                contentKey = uploadFileContentKey,
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

    private suspend fun UploadFileLink.addUploadBlocks(uploadBlocks: List<UploadBlock>) =
        addUploadBlocks(id, uploadBlocks)
}
