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

package me.proton.core.drive.crypto.domain.usecase.document

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.crypto.domain.usecase.file.AvoidDuplicateFileName
import me.proton.core.drive.crypto.domain.usecase.upload.ManifestSignature
import me.proton.core.drive.cryptobase.domain.usecase.EncryptText
import me.proton.core.drive.document.base.domain.entity.DocumentType
import me.proton.core.drive.document.base.domain.entity.NewDocumentInfo
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
import me.proton.core.drive.share.domain.usecase.GetSignatureAddress
import javax.inject.Inject

class CreateNewDocumentInfo @Inject constructor(
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
    private val getSignatureAddress: GetSignatureAddress,
    private val generateNodeKey: GenerateNodeKey,
    private val generateContentKey: GenerateContentKey,
    private val manifestSignature: ManifestSignature,
    private val getAddressKeys: GetAddressKeys,
    private val hmacSha256: HmacSha256,
    private val encryptText: EncryptText,
    private val avoidDuplicateFileName: AvoidDuplicateFileName,
    private val validateLinkName: ValidateLinkName,
) {

    suspend operator fun invoke(
        folder: Link.Folder,
        name: String,
        documentType: DocumentType,
    ): Result<NewDocumentInfo> = coRunCatching {
        val folderKey = getNodeKey(folder).getOrThrow()
        val folderHashKey = getNodeHashKey(folder, folderKey).getOrThrow()
        val userId = folder.userId
        val signatureAddress = getSignatureAddress(folder.shareId).getOrThrow()
        val documentKey = generateNodeKey(userId, folderKey, signatureAddress).getOrThrow()
        val documentContentKey = generateContentKey(documentKey).getOrThrow()
        val addressKey = getAddressKeys(userId, signatureAddress)
        val documentName = avoidDuplicateFileName(
            fileName = validateLinkName(name).getOrThrow(),
            parentFolderId = folder.id,
            folderHashKey = folderHashKey,
        ).getOrThrow()
        NewDocumentInfo(
            parentId = folder.id,
            encryptedName = encryptText(
                encryptKey = folderKey.keyHolder,
                text = documentName,
                signKey = addressKey.keyHolder,
            ).getOrThrow(),
            hash = hmacSha256(folderHashKey, documentName).getOrThrow(),
            nodeKey = documentKey.nodeKey,
            nodePassphrase = documentKey.nodePassphrase,
            nodePassphraseSignature = documentKey.nodePassphraseSignature,
            signatureAddress = signatureAddress,
            contentKeyPacket = documentContentKey.contentKeyPacket,
            contentKeyPacketSignature = documentContentKey.contentKeyPacketSignature,
            manifestSignature = manifestSignature(addressKey).getOrThrow(),
            documentType = documentType,
        )
    }
}
