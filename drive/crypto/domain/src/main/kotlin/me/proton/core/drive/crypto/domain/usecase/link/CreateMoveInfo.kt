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
package me.proton.core.drive.crypto.domain.usecase.link

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.DecryptLinkName
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.cryptobase.domain.usecase.ChangeMessage
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.extension.nodePassphrase
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.key.domain.usecase.MoveNodeKey
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.MoveInfo
import me.proton.core.drive.link.domain.extension.requireParentId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.usecase.GetSignatureAddress
import javax.inject.Inject

class CreateMoveInfo @Inject constructor(
    private val getSignatureAddress: GetSignatureAddress,
    private val getAddressKeys: GetAddressKeys,
    private val getLink: GetLink,
    private val changeMessage: ChangeMessage,
    private val decryptLinkName: DecryptLinkName,
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
    private val hmacSha256: HmacSha256,
    private val moveNodeKey: MoveNodeKey,
) {
    suspend operator fun invoke(
        linkId: LinkId,
        newParentId: FolderId,
    ): Result<MoveInfo> = coRunCatching {
        val link = getLink(linkId).toResult().getOrThrow()
        val decryptedLinkName = decryptLinkName(link).getOrThrow().text
        val currentParentFolder = getLink(link.requireParentId()).toResult().getOrThrow()
        val currentParentFolderKey = getNodeKey(currentParentFolder).getOrThrow()
        val newParentFolder = getLink(newParentId).toResult().getOrThrow()
        val newParentFolderKey = getNodeKey(newParentFolder).getOrThrow()
        val newParentFolderHashKey = getNodeHashKey(newParentFolder, newParentFolderKey).getOrThrow()
        val userId = linkId.userId
        val signatureAddress = getSignatureAddress(link.shareId).getOrThrow()
        val newLinkKey = moveNodeKey(
            userId = userId,
            key = getNodeKey(linkId).getOrThrow(),
            oldParentKey = currentParentFolderKey,
            newParentKey = newParentFolderKey,
            signatureAddress = signatureAddress,
        ).getOrThrow()
        MoveInfo(
            name = changeMessage(
                oldMessage = link.name,
                oldMessageDecryptionKey = currentParentFolderKey.keyHolder,
                newMessage = decryptedLinkName,
                newMessageEncryptionKey = newParentFolderKey.keyHolder,
                signKey = getAddressKeys(userId, signatureAddress).keyHolder,
            ).getOrThrow(),
            hash = hmacSha256(newParentFolderHashKey, decryptedLinkName).getOrThrow(),// calculate with new parent,
            previousHash = link.hash,
            parentLinkId = newParentFolder.id.id,
            nodePassphrase = newLinkKey.nodePassphrase,
            nameSignatureEmail = signatureAddress,
        )
    }
}
