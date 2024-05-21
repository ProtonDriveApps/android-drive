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
package me.proton.core.drive.crypto.domain.usecase

import me.proton.core.crypto.common.pgp.DecryptedText
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.cryptobase.domain.usecase.DecryptAndVerifyText
import me.proton.core.drive.cryptobase.domain.usecase.GetPublicKeyRing
import me.proton.core.drive.cryptobase.domain.usecase.UnlockKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.key.domain.usecase.GetPublicAddressKeys
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.File
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.shareId
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject

class DecryptLinkXAttr @Inject constructor(
    private val getLink: GetLink,
    private val getLinkKey: GetNodeKey,
    private val decryptAndVerifyText: DecryptAndVerifyText,
    private val getPublicAddressKeys: GetPublicAddressKeys,
    private val getPublicKeyRing: GetPublicKeyRing,
    private val unlockKey: UnlockKey,
) {
    suspend operator fun invoke(link: BaseLink): Result<DecryptedText> = getLinkKey(link.id).mapCatching { decryptKey ->
        val signatureAddress = when (link) {
            is File -> link.uploadedBy
            is Folder -> link.signatureAddress
            else -> throw IllegalStateException("Link must be either file or folder and it was not")
        }
        unlockKey(decryptKey.keyHolder) { unlockedKey ->
            decryptAndVerifyText(
                unlockedKey = unlockedKey,
                text = requireNotNull(link.xAttr),
                verifyKeyRing = getPublicKeyRing(
                    getPublicAddressKeys(link.shareId.userId, signatureAddress).getOrThrow().keyHolder
                ).getOrThrow(),
                verificationFailedContext = javaClass.simpleName,
            ).getOrThrow()
        }.getOrThrow()
    }

    suspend operator fun invoke(linkId: LinkId): Result<DecryptedText> = getLink(linkId)
        .toResult()
        .mapCatching { link ->
            invoke(link).getOrThrow()
        }
}
