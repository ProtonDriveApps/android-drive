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

package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkName
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.cryptobase.domain.usecase.ChangeMessage
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.NodeHashKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.extension.nodePassphrase
import me.proton.core.drive.key.domain.extension.nodePassphraseSignature
import me.proton.core.drive.key.domain.extension.signatureEmail
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.key.domain.usecase.MoveNodeKey
import me.proton.core.drive.link.domain.entity.CopyInfo
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.ParentId
import me.proton.core.drive.link.domain.extension.isPhoto
import me.proton.core.drive.link.domain.extension.requireParentId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.usecase.GetSignatureAddress
import me.proton.core.drive.volume.domain.entity.VolumeId
import javax.inject.Inject

class CreateCopyInfo @Inject constructor(
    private val getLink: GetLink,
    private val decryptLinkName: DecryptLinkName,
    private val changeMessage: ChangeMessage,
    private val getSignatureAddress: GetSignatureAddress,
    private val getAddressKeys: GetAddressKeys,
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
    private val hmacSha256: HmacSha256,
    private val moveNodeKey: MoveNodeKey,
    private val getContentHash: GetContentHash,
) {

    suspend operator fun invoke(
        newVolumeId: VolumeId,
        newParentId: ParentId,
        fileId: FileId,
        relatedPhotoIds: List<FileId>,
        contentDigestMap: Map<FileId, String?> = emptyMap(),
    ): Result<CopyInfo> = coRunCatching {
        val userId = fileId.userId
        val file = getLink(fileId).toResult().getOrThrow()
        val currentParent = getLink(file.requireParentId()).toResult().getOrThrow()
        val currentParentKey = getNodeKey(currentParent).getOrThrow()
        val newParent = getLink(newParentId).toResult().getOrThrow()
        val newParentKey = getNodeKey(newParent).getOrThrow()
        val newParentHashKey = when(newParent) {
            is Link.Album -> getNodeHashKey(newParent, newParentKey).getOrThrow()
            is Link.Folder -> getNodeHashKey(newParent, newParentKey).getOrThrow()
            else -> error("Either folder of album can be parent")
        }
        val decryptedFileName = decryptLinkName(file).getOrThrow().text
        val signatureAddress = getSignatureAddress(file.shareId).getOrThrow()
        val newLinkKey = moveNodeKey(
            userId = userId,
            key = getNodeKey(file).getOrThrow(),
            oldParentKey = currentParentKey,
            newParentKey = newParentKey,
            signatureAddress = signatureAddress,
        ).getOrThrow()
        val name = changeMessage(
            oldMessage = file.name,
            oldMessageDecryptionKey = currentParentKey.keyHolder,
            newMessage = decryptedFileName,
            newMessageEncryptionKey = newParentKey.keyHolder,
            signKey = getAddressKeys(userId, signatureAddress).keyHolder,
        ).getOrThrow()
        val hash = hmacSha256(newParentHashKey, decryptedFileName).getOrThrow()
        val nodePassphrase = newLinkKey.nodePassphrase
        val nodePassphraseSignature =file.nodePassphraseSignature(newLinkKey)
        val signatureEmail = file.signatureEmail(signatureAddress)
        if (file.isPhoto) {
            CopyInfo.Photo(
                name = name,
                hash = hash,
                targetVolumeId = newVolumeId.id,
                targetParentLinkId = newParentId.id,
                nodePassphrase = nodePassphrase,
                nameSignatureEmail = signatureAddress,
                nodePassphraseSignature = nodePassphraseSignature,
                signatureEmail = signatureEmail,
                photos = CopyInfo.Photo.Photos(
                    contentHash = contentDigestMap[fileId]?.let { contentDigest ->
                        getContentHash(newParentHashKey, contentDigest).getOrThrow()
                    } ?: requireNotNull(file.photoContentHash),
                    relatedPhotos = relatedPhotoIds.map { relatedPhotoId ->
                        createRelatePhotoCopyInfo(
                            relatedPhotoId = relatedPhotoId,
                            currentParentKey = currentParentKey,
                            newParentKey = newParentKey,
                            newParentHashKey = newParentHashKey,
                            contentDigest = contentDigestMap[relatedPhotoId],
                        )
                    }
                )
            )
        } else {
            CopyInfo.File(
                name = name,
                hash = hash,
                targetVolumeId = newVolumeId.id,
                targetParentLinkId = newParentId.id,
                nodePassphrase = nodePassphrase,
                nameSignatureEmail = signatureAddress,
                nodePassphraseSignature = nodePassphraseSignature,
                signatureEmail = signatureEmail,
            )
        }
    }

    private suspend fun createRelatePhotoCopyInfo(
        relatedPhotoId: FileId,
        currentParentKey: Key.Node,
        newParentKey: Key.Node,
        newParentHashKey: NodeHashKey,
        contentDigest: String?
    ): CopyInfo.Photo.RelatedPhoto {
        val userId = relatedPhotoId.userId
        val file = getLink(relatedPhotoId).toResult().getOrThrow()
        val decryptedFileName = decryptLinkName(file).getOrThrow().text
        val signatureAddress = getSignatureAddress(file.shareId).getOrThrow()
        val newLinkKey = moveNodeKey(
            userId = userId,
            key = getNodeKey(file).getOrThrow(),
            oldParentKey = currentParentKey,
            newParentKey = newParentKey,
            signatureAddress = signatureAddress,
        ).getOrThrow()
        val name = changeMessage(
            oldMessage = file.name,
            oldMessageDecryptionKey = currentParentKey.keyHolder,
            newMessage = decryptedFileName,
            newMessageEncryptionKey = newParentKey.keyHolder,
            signKey = getAddressKeys(userId, signatureAddress).keyHolder,
        ).getOrThrow()
        val hash = hmacSha256(newParentHashKey, decryptedFileName).getOrThrow()
        val nodePassphrase = newLinkKey.nodePassphrase
        return CopyInfo.Photo.RelatedPhoto(
            relatedPhotoId.id,
            name = name,
            hash = hash,
            nodePassphrase = nodePassphrase,
            contentHash = contentDigest?.let {
                getContentHash(newParentHashKey, contentDigest).getOrThrow()
            } ?: requireNotNull(file.photoContentHash),
        )
    }
}
