/*
 * Copyright (c) 2023 Proton AG.
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

import me.proton.android.drive.verifier.domain.usecase.BuildVerifier
import me.proton.android.drive.verifier.domain.usecase.CleanupVerifier
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetSignatureAddress
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.BuildNodeKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.UpdateVerifierToken
import javax.inject.Inject

class VerifyBlocks @Inject constructor(
    private val buildVerifier: BuildVerifier,
    private val cleanupVerifier: CleanupVerifier,
    private val getUploadBlocks: GetUploadBlocks,
    private val getSignatureAddress: GetSignatureAddress,
    private val getNodeKey: GetNodeKey,
    private val buildNodeKey: BuildNodeKey,
    private val updateVerifierToken: UpdateVerifierToken,
    private val configurationProvider: ConfigurationProvider,
) {
    suspend operator fun invoke(uploadFileLink: UploadFileLink): Result<Unit> = coRunCatching {
        if (!configurationProvider.useVerifier) return@coRunCatching
        val verifier = buildVerifier(
            userId = uploadFileLink.userId,
            shareId = uploadFileLink.shareId.id,
            linkId = requireNotNull(uploadFileLink.linkId),
            revisionId = uploadFileLink.draftRevisionId,
            fileKey = uploadFileLink.buildFileKey(),
        ).getOrThrow()
        val uploadBlocks = getUploadBlocks(uploadFileLink)
            .getOrThrow()
            .associateBy { uploadBlock -> uploadBlock.file }
        verifier.verifyBlocks(uploadBlocks.keys.toList())
            .getOrThrow()
            .map { (file, verifierToken) ->
                updateVerifierToken(
                    uploadFileLinkId = uploadFileLink.id,
                    index = requireNotNull(uploadBlocks[file]).index,
                    verifierToken = verifierToken,
                )
            }
        cleanupVerifier(
            userId = uploadFileLink.userId,
            shareId = uploadFileLink.shareId.id,
            linkId = requireNotNull(uploadFileLink.linkId),
            revisionId = uploadFileLink.draftRevisionId,
        )
    }

    private suspend fun UploadFileLink.buildFileKey(): Key.Node =
        buildNodeKey(
            userId = userId,
            parentKey = getNodeKey(parentLinkId).getOrThrow(),
            uploadFileLink = this,
            signatureAddress = getSignatureAddress(userId),
        ).getOrThrow()
}
