/*
 * Copyright (c) 2022-2024 Proton AG.
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
package me.proton.core.drive.crypto.domain.usecase.share

import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.base.ReencryptKeyPacket
import me.proton.core.drive.key.domain.extension.nodeKey
import me.proton.core.drive.key.domain.extension.nodePassphrase
import me.proton.core.drive.key.domain.extension.nodePassphraseSignature
import me.proton.core.drive.key.domain.usecase.GenerateShareKey
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.drive.share.domain.entity.ShareInfo
import me.proton.core.drive.share.domain.usecase.GetAddressId
import me.proton.core.drive.share.domain.usecase.GetShare
import javax.inject.Inject

class CreateShareInfo @Inject constructor(
    private val getAddressId: GetAddressId,
    private val generateShareKey: GenerateShareKey,
    private val reencryptKeyPacket: ReencryptKeyPacket,
    private val getShare: GetShare,
    private val getLink: GetLink,
) {
    suspend operator fun invoke(linkId: LinkId, name: String): Result<ShareInfo> = coRunCatching {
        val userId = linkId.shareId.userId
        val link = getLink(linkId).toResult().getOrThrow()
        val volumeId = getShare(linkId.shareId).toResult().getOrThrow().volumeId
        val addressId = getAddressId(linkId.userId, volumeId).getOrThrow()
        val shareKey = generateShareKey(userId, addressId, linkId).getOrThrow()
        ShareInfo(
            addressId = addressId,
            name = name,
            rootLinkId = linkId.id,
            shareKey = shareKey.nodeKey,
            sharePassphrase = shareKey.nodePassphrase,
            sharePassphraseSignature = shareKey.nodePassphraseSignature,
            passphraseKeyPacket = reencryptKeyPacket(
                message = link.nodePassphrase,
                link = link,
                shareKey = shareKey
            ).getOrThrow(),
            nameKeyPacket = reencryptKeyPacket(
                message = link.name,
                link = link,
                shareKey = shareKey,
            ).getOrThrow(),
        )
    }
}
