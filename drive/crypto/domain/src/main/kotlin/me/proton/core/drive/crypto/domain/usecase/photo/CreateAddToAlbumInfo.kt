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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkName
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.crypto.domain.usecase.file.GetContentHash
import me.proton.core.drive.cryptobase.domain.usecase.ChangeMessage
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.extension.nodePassphrase
import me.proton.core.drive.key.domain.extension.nodePassphraseSignature
import me.proton.core.drive.key.domain.extension.signatureEmail
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.key.domain.usecase.MoveNodeKey
import me.proton.core.drive.link.domain.entity.AlbumId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.extension.requireParentId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.photo.domain.entity.AddToAlbumInfo
import me.proton.core.drive.share.domain.usecase.GetSignatureAddress
import javax.inject.Inject

class CreateAddToAlbumInfo @Inject constructor(
    private val getLink: GetLink,
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
    private val getAddressKeys: GetAddressKeys,
    private val getSignatureAddress: GetSignatureAddress,
    private val decryptLinkName: DecryptLinkName,
    private val changeMessage: ChangeMessage,
    private val hmacSha256: HmacSha256,
    private val moveNodeKey: MoveNodeKey,
    private val getContentHash: GetContentHash,
    private val configurationProvider: ConfigurationProvider,
) {

    suspend operator fun invoke(
        photoId: FileId,
        albumId: AlbumId,
        contentDigest: String?,
    ): Result<AddToAlbumInfo> = coRunCatching {
        val album = getLink(albumId).toResult().getOrThrow()
        val albumKey = getNodeKey(album).getOrThrow()
        val photo = getLink(photoId).toResult().getOrThrow()
        val decryptedPhotoName = decryptLinkName(photo).getOrThrow().text
        val currentParentFolder = getLink(photo.requireParentId()).toResult().getOrThrow()
        val currentParentFolderKey = getNodeKey(currentParentFolder).getOrThrow()
        val albumHashKey = getNodeHashKey(album, albumKey).getOrThrow()
        val signatureAddress = getSignatureAddress(album.shareId).getOrThrow()
        val newPhotoKey = moveNodeKey(
            userId = album.userId,
            key = getNodeKey(photoId).getOrThrow(),
            oldParentKey = currentParentFolderKey,
            newParentKey = albumKey,
            signatureAddress = signatureAddress,
        ).getOrThrow()
        AddToAlbumInfo(
            linkId = photoId.id,
            name = changeMessage(
                oldMessage = photo.name,
                oldMessageDecryptionKey = currentParentFolderKey.keyHolder,
                newMessage = decryptedPhotoName,
                newMessageEncryptionKey = albumKey.keyHolder,
                signKey = getAddressKeys(albumId.userId, signatureAddress).keyHolder,
            ).getOrThrow(),
            hash = hmacSha256(albumHashKey, decryptedPhotoName).getOrThrow(),
            nameSignatureEmail = signatureAddress,
            nodePassphrase = newPhotoKey.nodePassphrase,
            nodePassphraseSignature = photo.nodePassphraseSignature(newPhotoKey),
            signatureEmail = photo.signatureEmail(signatureAddress),
            contentHash = contentDigest?.let {
                getContentHash(albumHashKey, contentDigest).getOrThrow()
            } ?: requireNotNull(photo.photoContentHash),
        )
    }

    suspend operator fun invoke(
        photoIds: List<FileId>,
        albumId: AlbumId,
        contentDigests: Map<FileId, String?>,
    ): Result<List<AddToAlbumInfo>> = withContext(Dispatchers.IO) {
        coRunCatching {
            val album = getLink(albumId).toResult().getOrThrow()
            val albumKey = getNodeKey(album).getOrThrow()
            val albumHashKey = getNodeHashKey(album, albumKey).getOrThrow()
            val signatureAddress = getSignatureAddress(album.shareId).getOrThrow()
            val addressKeys = getAddressKeys(albumId.userId, signatureAddress).keyHolder
            photoIds.chunked(configurationProvider.contentDigestsInParallel)
                .flatMap { chunk ->
                    chunk.map { photoId ->
                        async {
                            val photo = getLink(photoId).toResult().getOrThrow()
                            val decryptedPhotoName = decryptLinkName(photo).getOrThrow().text
                            val currentParentFolder =
                                getLink(photo.requireParentId()).toResult().getOrThrow()
                            val currentParentFolderKey =
                                getNodeKey(currentParentFolder).getOrThrow()
                            val newPhotoKey = moveNodeKey(
                                userId = album.userId,
                                key = getNodeKey(photoId).getOrThrow(),
                                oldParentKey = currentParentFolderKey,
                                newParentKey = albumKey,
                                signatureAddress = signatureAddress,
                            ).getOrThrow()
                            AddToAlbumInfo(
                                linkId = photoId.id,
                                name = changeMessage(
                                    oldMessage = photo.name,
                                    oldMessageDecryptionKey = currentParentFolderKey.keyHolder,
                                    newMessage = decryptedPhotoName,
                                    newMessageEncryptionKey = albumKey.keyHolder,
                                    signKey = addressKeys,
                                ).getOrThrow(),
                                hash = hmacSha256(albumHashKey, decryptedPhotoName).getOrThrow(),
                                nameSignatureEmail = signatureAddress,
                                nodePassphrase = newPhotoKey.nodePassphrase,
                                nodePassphraseSignature = photo.nodePassphraseSignature(newPhotoKey),
                                signatureEmail = photo.signatureEmail(signatureAddress),
                                contentHash = contentDigests[photoId]?.let { contentDigest ->
                                    getContentHash(albumHashKey, contentDigest).getOrThrow()
                                } ?: requireNotNull(photo.photoContentHash),
                            )
                        }
                    }.awaitAll()
                }
        }
    }
}
