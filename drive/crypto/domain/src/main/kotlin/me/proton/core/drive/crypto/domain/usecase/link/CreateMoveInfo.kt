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
import me.proton.core.drive.key.domain.extension.nodePassphraseSignature
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.key.domain.usecase.MoveNodeKey
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.entity.MoveInfo
import me.proton.core.drive.link.domain.entity.ParentId
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
        newParentId: ParentId,
    ): Result<MoveInfo> = coRunCatching {
        val link = getLink(linkId).toResult().getOrThrow()
        val decryptedLinkName = decryptLinkName(link).getOrThrow().text
        val currentParent = getLink(link.requireParentId()).toResult().getOrThrow()
        val currentParentKey = getNodeKey(currentParent).getOrThrow()
        val newParent = getLink(newParentId).toResult().getOrThrow()
        val newParentKey = getNodeKey(newParent).getOrThrow()
        val newParentHashKey = when(newParent) {
            is Link.Album -> getNodeHashKey(newParent, newParentKey).getOrThrow()
            is Link.Folder -> getNodeHashKey(newParent, newParentKey).getOrThrow()
            else -> error("Either folder of album can be parent")
        }
        val userId = linkId.userId
        val signatureAddress = getSignatureAddress(link.shareId).getOrThrow()
        val newLinkKey = moveNodeKey(
            userId = userId,
            key = getNodeKey(linkId).getOrThrow(),
            oldParentKey = currentParentKey,
            newParentKey = newParentKey,
            signatureAddress = signatureAddress,
        ).getOrThrow()
        MoveInfo(
            name = changeMessage(
                oldMessage = link.name,
                oldMessageDecryptionKey = currentParentKey.keyHolder,
                newMessage = decryptedLinkName,
                newMessageEncryptionKey = newParentKey.keyHolder,
                signKey = getAddressKeys(userId, signatureAddress).keyHolder,
            ).getOrThrow(),
            hash = hmacSha256(newParentHashKey, decryptedLinkName).getOrThrow(),// calculate with new parent,
            previousHash = link.hash,
            parentLinkId = newParent.id.id,
            signatureEmail = if (link.signatureEmail.isEmpty()) {
                signatureAddress
            } else {
                null
            },
            nodePassphrase = newLinkKey.nodePassphrase,
            nodePassphraseSignature = if (link.signatureEmail.isEmpty() || link.nameSignatureEmail.isNullOrEmpty()) {
                newLinkKey.nodePassphraseSignature
            } else {
                null
            },
            nameSignatureEmail = signatureAddress,
        )
    }
}
