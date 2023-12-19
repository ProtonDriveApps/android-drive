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

import kotlinx.coroutines.flow.flowOf
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.entity.toTimestampS
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetSignatureAddress
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.GetContentHash
import me.proton.core.drive.crypto.domain.usecase.file.VerifyManifestSignaturePrimary
import me.proton.core.drive.crypto.domain.usecase.link.EncryptAndSignXAttr
import me.proton.core.drive.cryptobase.domain.exception.VerificationException
import me.proton.core.drive.file.base.domain.entity.PhotoAttributes
import me.proton.core.drive.file.base.domain.entity.RevisionState
import me.proton.core.drive.file.base.domain.usecase.CreateXAttr
import me.proton.core.drive.file.base.domain.usecase.GetRevision
import me.proton.core.drive.file.base.domain.usecase.UpdateRevision
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.BuildNodeKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.extension.isThumbnail
import me.proton.core.drive.linkupload.domain.extension.requireFileId
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.util.kotlin.CoreLogger
import java.util.Date
import javax.inject.Inject

@Suppress("LongParameterList")
class UpdateRevision @Inject constructor(
    private val getRevision: GetRevision,
    private val updateRevision: UpdateRevision,
    private val getSignatureAddress: GetSignatureAddress,
    private val linkUploadRepository: LinkUploadRepository,
    private val updateUploadState: UpdateUploadState,
    private val createXAttr: CreateXAttr,
    private val encryptAndSignXAttr: EncryptAndSignXAttr,
    private val buildNodeKey: BuildNodeKey,
    private val getNodeKey: GetNodeKey,
    private val getContentHash: GetContentHash,
    private val getShare: GetShare,
    private val verifyManifestSignaturePrimary: VerifyManifestSignaturePrimary,
    private val configurationProvider: ConfigurationProvider,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke(
        uploadFileLink: UploadFileLink,
        checkRevisionState: Boolean = false,
    ): Result<UploadFileLink> = coRunCatching {
        with(uploadFileLink) {
            if (checkRevisionState) {
                val revision = getRevision(requireFileId(), draftRevisionId).getOrThrow()
                if (revision.state != RevisionState.DRAFT) {
                    CoreLogger.d(LogTag.UPLOAD, "Ignoring already committed revision: $draftRevisionId")
                    return@with uploadFileLink
                }
            }
            updateUploadState(id, UploadState.UPDATING_REVISION).getOrThrow()
            try {
                val fileId = requireFileId()
                val folderKey = getNodeKey(parentLinkId).getOrThrow()
                val signatureAddress = getSignatureAddress(userId)
                val uploadFileKey = buildNodeKey(userId, folderKey, this, signatureAddress).getOrThrow()
                val uploadBlocks = linkUploadRepository.getUploadBlocks(this)
                val manifestSignatureVerified = verifyManifestSignaturePrimary(
                    userId = userId,
                    signatureAddress = signatureAddress,
                    blocks = uploadBlocks,
                    manifestSignature = manifestSignature,
                ).getOrThrow()
                if (!manifestSignatureVerified) {
                    throw VerificationException("Cannot verify manifest signature with primary address key")
                }
                updateRevision(
                    fileId = fileId,
                    revisionId = draftRevisionId,
                    manifestSignature = manifestSignature,
                    signatureAddress = signatureAddress,
                    blockNumber = uploadBlocks.size.toLong(),
                    xAttr = encryptAndSignXAttr(
                        userId = userId,
                        encryptKey = uploadFileKey,
                        signatureAddress = signatureAddress,
                        xAttr = createXAttr(
                            modificationTime = Date(lastModified?.value ?: System.currentTimeMillis()),
                            size = requireNotNull(size),
                            blockSizes = uploadBlocks
                                .filterNot { uploadBlock -> uploadBlock.isThumbnail }
                                .map { uploadBlock -> uploadBlock.rawSize },
                            mediaResolution = mediaResolution,
                            mediaDuration = mediaDuration,
                            creationDateTime = fileCreationDateTime,
                            digests = digests.values,
                            cameraExifTags = cameraExifTags,
                        ),
                    ).getOrThrow(),
                    photoAttributes = createPhotoAttributes(
                        uploadFileLink = this,
                        share = getShare(shareId, flowOf(false)).toResult().getOrThrow(),
                        folderKey = folderKey,
                    ),
                ).getOrThrow()
                uploadFileLink
            } catch (e: Throwable) {
                updateUploadState(id, UploadState.IDLE)
                throw e
            }
        }
    }

    private suspend fun createPhotoAttributes(
        uploadFileLink: UploadFileLink,
        share: Share,
        folderKey: Key.Node,
    ): PhotoAttributes? = takeIf { share.type == Share.Type.PHOTO }?.let {
        with (uploadFileLink) {
            PhotoAttributes(
                captureTime = fileCreationDateTime
                    ?: lastModified?.toTimestampS()
                    ?: TimestampS().also { currentTime ->
                        with(LogTag.UploadTag) {
                            CoreLogger.d(
                                id.logTag(),
                                "No capture time or last modified for $uriString, using current time:$currentTime"
                            )
                        }
                    },
                contentHash = digests.values.getValue(configurationProvider.contentDigestAlgorithm)
                    .let { contentDigest ->
                        getContentHash(parentLinkId, folderKey, contentDigest).getOrThrow()
                    },
            )
        }
    }
}
