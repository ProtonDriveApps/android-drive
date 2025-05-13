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

package me.proton.core.drive.crypto.domain.usecase.photo

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkName
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.crypto.domain.usecase.file.GetContentHash
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
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.requireParentId
import me.proton.core.drive.link.domain.extension.rootFolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.photo.domain.entity.PhotoFavoriteInfo
import me.proton.core.drive.photo.domain.entity.RelatedPhotoFavoriteInfo
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.domain.usecase.GetSignatureAddress
import javax.inject.Inject

class CreatePhotoFavoriteInfo @Inject constructor(
    private val getLink: GetLink,
    private val getShare: GetShare,
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
    private val getAddressKeys: GetAddressKeys,
    private val getSignatureAddress: GetSignatureAddress,
    private val decryptLinkName: DecryptLinkName,
    private val changeMessage: ChangeMessage,
    private val hmacSha256: HmacSha256,
    private val moveNodeKey: MoveNodeKey,
    private val getContentHash: GetContentHash,
) {
    suspend operator fun invoke(
        photoId: FileId,
        shareId: ShareId,
        contentDigestMap: Map<FileId, String?>,
        relatedPhotoIds: List<FileId>,
    ): Result<PhotoFavoriteInfo?> = coRunCatching {
        val share = getShare(shareId).toResult().getOrThrow()
        val rootFolder = getLink(share.rootFolderId).toResult().getOrThrow()
        val rootFolderKey = getNodeKey(rootFolder).getOrThrow()
        val rootFolderHashKey = getNodeHashKey(rootFolder, rootFolderKey).getOrThrow()
        val photo = getLink(photoId).toResult().getOrThrow()
        val decryptedPhotoName = decryptLinkName(photo).getOrThrow().text
        val currentParentFolder = getLink(photo.requireParentId()).toResult().getOrThrow()
        val currentParentFolderKey = getNodeKey(currentParentFolder).getOrThrow()
        val signatureAddress = getSignatureAddress(shareId).getOrThrow()
        val newPhotoKey = moveNodeKey(
            userId = rootFolder.userId,
            key = getNodeKey(photoId).getOrThrow(),
            oldParentKey = currentParentFolderKey,
            newParentKey = rootFolderKey,
            signatureAddress = signatureAddress,
        ).getOrThrow()
        PhotoFavoriteInfo(
            hash = hmacSha256(rootFolderHashKey, decryptedPhotoName).getOrThrow(),
            name = changeMessage(
                oldMessage = photo.name,
                oldMessageDecryptionKey = currentParentFolderKey.keyHolder,
                newMessage = decryptedPhotoName,
                newMessageEncryptionKey = rootFolderKey.keyHolder,
                signKey = getAddressKeys(shareId.userId, signatureAddress).keyHolder,
            ).getOrThrow(),
            nameSignatureEmail = signatureAddress,
            nodePassphrase = newPhotoKey.nodePassphrase,
            nodePassphraseSignature = photo.nodePassphraseSignature(newPhotoKey),
            signatureEmail = photo.signatureEmail(signatureAddress),
            contentHash = contentDigestMap[photoId]?.let { contentDigest ->
                getContentHash(rootFolderHashKey, contentDigest).getOrThrow()
            } ?: requireNotNull(photo.photoContentHash),
            relatedPhotoFavoriteInfos = relatedPhotoIds.map { relatedPhotoId ->
                createRelatedPhotoFavoriteInfo(
                    relatedPhotoId = relatedPhotoId,
                    currentParentFolderKey = currentParentFolderKey,
                    rootFolderKey = rootFolderKey,
                    rootFolderHashKey = rootFolderHashKey,
                    signatureAddress = signatureAddress,
                    contentDigest = contentDigestMap[relatedPhotoId],
                )
            }
        )
    }

    private suspend fun createRelatedPhotoFavoriteInfo(
        relatedPhotoId: FileId,
        currentParentFolderKey: Key.Node,
        rootFolderKey: Key.Node,
        rootFolderHashKey: NodeHashKey,
        signatureAddress: String,
        contentDigest: String?
    ): RelatedPhotoFavoriteInfo {
        val userId = relatedPhotoId.userId
        val photo = getLink(relatedPhotoId).toResult().getOrThrow()
        val decryptedPhotoName = decryptLinkName(photo).getOrThrow().text
        val newPhotoKey = moveNodeKey(
            userId = userId,
            key = getNodeKey(relatedPhotoId).getOrThrow(),
            oldParentKey = currentParentFolderKey,
            newParentKey = rootFolderKey,
            signatureAddress = signatureAddress,
        ).getOrThrow()
        return RelatedPhotoFavoriteInfo(
            linkId = relatedPhotoId,
            hash = hmacSha256(rootFolderHashKey, decryptedPhotoName).getOrThrow(),
            name = changeMessage(
                oldMessage = photo.name,
                oldMessageDecryptionKey = currentParentFolderKey.keyHolder,
                newMessage = decryptedPhotoName,
                newMessageEncryptionKey = rootFolderKey.keyHolder,
                signKey = getAddressKeys(
                    userId,
                    signatureAddress
                ).keyHolder,
            ).getOrThrow(),
            nameSignatureEmail = signatureAddress,
            nodePassphrase = newPhotoKey.nodePassphrase,
            nodePassphraseSignature = photo.nodePassphraseSignature(newPhotoKey),
            signatureEmail = photo.signatureEmail(signatureAddress),
            contentHash = contentDigest?.let {
                getContentHash(rootFolderHashKey, contentDigest).getOrThrow()
            } ?: requireNotNull(photo.photoContentHash),
        )
    }
}
