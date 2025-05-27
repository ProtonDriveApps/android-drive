/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.crypto.domain.usecase.folder

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.crypto.domain.usecase.link.EncryptAndSignXAttr
import me.proton.core.drive.cryptobase.domain.usecase.EncryptAndSignText
import me.proton.core.drive.file.base.domain.usecase.CreateXAttr
import me.proton.core.drive.folder.domain.entity.FolderInfo
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.extension.nodeKey
import me.proton.core.drive.key.domain.extension.nodePassphrase
import me.proton.core.drive.key.domain.extension.nodePassphraseSignature
import me.proton.core.drive.key.domain.usecase.GenerateNodeHashKey
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

class CreateFolderInfo @Inject constructor(
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
    private val generateNodeKey: GenerateNodeKey,
    private val generateNodeHashKey: GenerateNodeHashKey,
    private val encryptAndSignText: EncryptAndSignText,
    private val validateLinkName: ValidateLinkName,
    private val hmacSha256: HmacSha256,
    private val getSignatureAddress: GetSignatureAddress,
    private val createXAttr: CreateXAttr,
    private val encryptAndSignXAttr: EncryptAndSignXAttr,
    private val getAddressKeys: GetAddressKeys,
) {
    suspend operator fun invoke(
        parentFolder: Link.Folder,
        name: String,
        nameValidator: (String) -> String = { validateLinkName(name).getOrThrow() },
    ): Result<Pair<String, FolderInfo>> = coRunCatching {
        val folderName = nameValidator(name)
        val parentFolderKey = getNodeKey(parentFolder).getOrThrow()
        val parentFolderHashKey = getNodeHashKey(parentFolder, parentFolderKey).getOrThrow()
        val userId = parentFolder.id.userId
        val signatureAddress = getSignatureAddress(parentFolder.shareId).getOrThrow()
        val folderKey = generateNodeKey(userId, parentFolderKey, signatureAddress).getOrThrow()
        val folderHashKey = generateNodeHashKey(folderKey).getOrThrow()
        folderName to FolderInfo(
            parentLinkId = parentFolder.id.id,
            name = encryptAndSignText(
                encryptKey = parentFolderKey.keyHolder,
                signKey = getAddressKeys(
                    userId = userId,
                    email = signatureAddress,
                ).keyHolder,
                text = folderName,
            ).getOrThrow(),
            hash = hmacSha256(parentFolderHashKey, folderName).getOrThrow(),
            nodeKey = folderKey.nodeKey,
            nodePassphrase = folderKey.nodePassphrase,
            nodePassphraseSignature = folderKey.nodePassphraseSignature,
            nodeHashKey = folderHashKey.encryptedHashKey,
            signatureAddress = signatureAddress,
            xAttr = encryptAndSignXAttr(userId, folderKey, signatureAddress, createXAttr()).getOrThrow(),
        )
    }
}
