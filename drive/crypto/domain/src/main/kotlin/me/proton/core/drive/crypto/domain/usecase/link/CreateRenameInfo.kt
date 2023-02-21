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

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.base.domain.usecase.GetSignatureAddress
import me.proton.core.drive.crypto.domain.usecase.HmacSha256
import me.proton.core.drive.cryptobase.domain.usecase.ChangeMessage
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import me.proton.core.drive.key.domain.usecase.GetNodeHashKey
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.RenameInfo
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.ValidateLinkName
import javax.inject.Inject

class CreateRenameInfo @Inject constructor(
    private val validateLinkName: ValidateLinkName,
    private val getAddressKeys: GetAddressKeys,
    private val changeMessage: ChangeMessage,
    private val getSignatureAddress: GetSignatureAddress,
    private val getNodeKey: GetNodeKey,
    private val getNodeHashKey: GetNodeHashKey,
    private val hmacSha256: HmacSha256,
) {
    suspend operator fun invoke(
        parentFolder: Link.Folder,
        link: Link,
        name: String,
        mimeType: String,
    ): Result<RenameInfo> = coRunCatching {
        val linkName = validateLinkName(name).getOrThrow()
        val parentFolderKey = getNodeKey(parentFolder).getOrThrow()
        val parentFolderHashKey = getNodeHashKey(parentFolder, parentFolderKey).getOrThrow()
        val signatureAddress = getSignatureAddress(link.userId)
        RenameInfo(
            name = changeMessage(
                oldMessage = link.name,
                oldMessageDecryptionKey = parentFolderKey.keyHolder,
                newMessage = linkName,
                signKey = getAddressKeys(link.userId, signatureAddress).keyHolder,
            ).getOrThrow(),
            hash = hmacSha256(parentFolderHashKey, linkName).getOrThrow(),
            previousHash = link.hash,
            mimeType = mimeType,
            signatureAddress = signatureAddress,
        )
    }
}
