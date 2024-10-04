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
package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.usecase.GetOrCreateClientUid
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.cryptobase.domain.usecase.EncryptText
import me.proton.core.drive.file.base.domain.entity.NewFileInfo
import me.proton.core.drive.key.domain.entity.ContentKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.extension.nodeKey
import me.proton.core.drive.key.domain.extension.nodePassphrase
import me.proton.core.drive.key.domain.extension.nodePassphraseSignature
import me.proton.core.drive.key.domain.usecase.GenerateContentKey
import me.proton.core.drive.key.domain.usecase.GenerateNodeKey
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.ValidateLinkName
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.share.domain.usecase.GetSignatureAddress
import javax.inject.Inject

class CreateNewFileInfo @Inject constructor(
    private val getAddressKeys: GetAddressKeys,
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
    private val generateNodeKey: GenerateNodeKey,
    private val generateContentKey: GenerateContentKey,
    private val encryptText: EncryptText,
    private val validateLinkName: ValidateLinkName,
    private val hmacSha256: HmacSha256,
    private val getSignatureAddress: GetSignatureAddress,
    private val avoidDuplicateFileName: AvoidDuplicateFileName,
    private val getOrCreateClientUid: GetOrCreateClientUid,
    private val getShare: GetShare,
) {

    suspend operator fun invoke(
        folder: Link.Folder,
        name: String,
        mimeType: String,
        fileKey: Key.Node,
        fileContentKey: ContentKey,
    ): Result<NewFileInfo> = coRunCatching {
        val folderKey = getNodeKey(folder).getOrThrow()
        val folderHashKey = getNodeHashKey(folder, folderKey).getOrThrow()
        val fileName = if (folder.allowDuplicateFileName()) {
            validateLinkName(name).getOrThrow()
        } else {
            avoidDuplicateFileName(
                fileName = validateLinkName(name).getOrThrow(),
                parentFolderId = folder.id,
                folderHashKey = folderHashKey,
            ).getOrThrow()
        }
        val userId = folder.userId
        val signatureAddress = getSignatureAddress(folder.shareId).getOrThrow()
        NewFileInfo(
            parentId = folder.id,
            name = fileName,
            encryptedName = encryptText(
                encryptKey = folderKey.keyHolder,
                text = fileName,
                signKey = getAddressKeys(userId, signatureAddress).keyHolder,
            ).getOrThrow(),
            hash = hmacSha256(folderHashKey, fileName).getOrThrow(),
            mimeType = mimeType,
            nodeKey = fileKey.nodeKey,
            nodePassphrase = fileKey.nodePassphrase,
            nodePassphraseSignature = fileKey.nodePassphraseSignature,
            signatureAddress = signatureAddress,
            contentKeyPacket = fileContentKey.contentKeyPacket,
            contentKeyPacketSignature = fileContentKey.contentKeyPacketSignature,
            clientUid = getOrCreateClientUid().getOrThrow()
        )
    }

    suspend operator fun invoke(
        folder: Link.Folder,
        name: String,
        mimeType: String,
    ): Result<NewFileInfo> = coRunCatching {
        val folderKey = getNodeKey(folder).getOrThrow()
        val userId = folder.userId
        val fileKey = generateNodeKey(
            userId = userId,
            parent = folderKey,
            signatureAddress = getSignatureAddress(folder.shareId).getOrThrow(),
        ).getOrThrow()
        invoke(
            folder = folder,
            name = name,
            mimeType = mimeType,
            fileKey = fileKey,
            fileContentKey = generateContentKey(fileKey).getOrThrow(),
        ).getOrThrow()
    }

    private suspend fun Link.Folder.allowDuplicateFileName() =
        getShare(id.shareId).toResult().getOrThrow().type == Share.Type.PHOTO
}
