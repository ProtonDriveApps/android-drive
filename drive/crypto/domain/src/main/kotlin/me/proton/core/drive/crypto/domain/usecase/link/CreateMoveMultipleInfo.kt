/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.crypto.domain.usecase.link

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.file.GetContentHash
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.MoveMultipleInfo
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.extension.isPhoto
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.usecase.GetSignatureAddress
import javax.inject.Inject

class CreateMoveMultipleInfo @Inject constructor(
    private val getLink: GetLink,
    private val getSignatureAddress: GetSignatureAddress,
    private val createMoveInfo: CreateMoveInfo,
    private val getContentHash: GetContentHash,
    private val getNodeHashKey: GetNodeHashKey,
) {

    suspend operator fun invoke(
        newParentId: ParentId,
        links: Set<LinkId>,
    ): Result<MoveMultipleInfo> = coRunCatching {
        val newParent = getLink(newParentId).toResult().getOrThrow()
        val signatureAddress = getSignatureAddress(newParent.id.shareId).getOrThrow()
        MoveMultipleInfo(
            parentLinkId = newParentId,
            links = links.toMoveInfos(newParentId),
            nameSignatureEmail = signatureAddress,
            signatureEmail = if (newParent.signatureEmail.isEmpty()) {
                signatureAddress
            } else {
                null
            },
        )
    }

    suspend operator fun invoke(
        newParentId: ParentId,
        linksContentDigests: Map<FileId, String?>,
    ): Result<MoveMultipleInfo> = coRunCatching {
        val newParent = getLink(newParentId).toResult().getOrThrow()
        val signatureAddress = getSignatureAddress(newParent.id.shareId).getOrThrow()
        MoveMultipleInfo(
            parentLinkId = newParentId,
            links = linksContentDigests.toMoveInfos(newParentId),
            nameSignatureEmail = signatureAddress,
            signatureEmail = if (newParent.signatureEmail.isEmpty()) {
                signatureAddress
            } else {
                null
            },
        )
    }

    private suspend fun Map<FileId, String?>.toMoveInfos(
        newParentId: ParentId,
    ): List<MoveMultipleInfo.MoveInfo> = map { (fileId, contentDigest) ->
        getLink(fileId)
            .toResult()
            .getOrThrow()
            .toPhotoFileMoveInfo(newParentId, contentDigest)
    }

    private suspend fun Link.toNonPhotoFileOrFolderMoveInfo(
        newParentId: ParentId,
    ): MoveMultipleInfo.MoveInfo.NonPhotoFileOrFolder {
        val moveInfo = createMoveInfo(id, newParentId).getOrThrow()
        return MoveMultipleInfo.MoveInfo.NonPhotoFileOrFolder(
            linkId = id,
            name = moveInfo.name,
            nodePassphrase = moveInfo.nodePassphrase,
            hash = moveInfo.hash,
            originalHash = moveInfo.previousHash,
            nodePassphraseSignature = moveInfo.nodePassphraseSignature,
        )
    }

    private suspend fun Set<LinkId>.toMoveInfos(
        newParentId: ParentId,
    ): List<MoveMultipleInfo.MoveInfo> = map { linkId ->
        when (val link = getLink(linkId).toResult().getOrThrow()) {
            is Link.File,
            is Link.Folder -> link.toNonPhotoFileOrFolderMoveInfo(newParentId)
            is Link.Album -> error("Cannot move albums")
        }
    }

    private suspend fun Link.File.toPhotoFileMoveInfo(
        newParentId: ParentId,
        contentDigest: String?,
    ): MoveMultipleInfo.MoveInfo.PhotoFile {
        check(isPhoto) { "Cannot move non-photo files" }
        val moveInfo = createMoveInfo(id, newParentId).getOrThrow()
        val nodeHashKey = when (newParentId) {
            is FolderId -> getNodeHashKey(
                getLink(newParentId).toResult().getOrThrow()
            ).getOrThrow()
            is AlbumId -> getNodeHashKey(
                getLink(newParentId).toResult().getOrThrow()
            ).getOrThrow()
        }
        return MoveMultipleInfo.MoveInfo.PhotoFile(
            linkId = id,
            name = moveInfo.name,
            nodePassphrase = moveInfo.nodePassphrase,
            hash = moveInfo.hash,
            originalHash = moveInfo.previousHash,
            contentHash = contentDigest?.let {
                getContentHash(nodeHashKey, contentDigest).getOrThrow()
            } ?: requireNotNull(photoContentHash),
            nodePassphraseSignature = moveInfo.nodePassphraseSignature,
        )
    }
}
