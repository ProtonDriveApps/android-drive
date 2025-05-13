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
import me.proton.core.drive.base.domain.usecase.Sha256
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.repository.DecryptedTextRepository
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.usecase.DecryptAndVerifyText
import me.proton.core.drive.cryptobase.domain.usecase.GetPublicKeyRing
import me.proton.core.drive.cryptobase.domain.usecase.UnlockKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.key.domain.usecase.GetVerificationKeys
import me.proton.core.drive.link.domain.entity.Album
import me.proton.core.drive.link.domain.entity.BaseLink
import me.proton.core.drive.link.domain.entity.File
import me.proton.core.drive.link.domain.entity.Folder
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.link.domain.extension.xAttrKey
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DecryptLinkXAttr @Inject constructor(
    private val getLink: GetLink,
    private val getLinkKey: GetNodeKey,
    private val decryptAndVerifyText: DecryptAndVerifyText,
    private val getVerificationKeys: GetVerificationKeys,
    private val getPublicKeyRing: GetPublicKeyRing,
    private val unlockKey: UnlockKey,
    private val decryptedTextRepository: DecryptedTextRepository,
    private val sha256: Sha256,
) {
    suspend operator fun invoke(
        link: BaseLink,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<DecryptedText> = coRunCatching {
        decryptedTextRepository.getDecryptedText(link.userId, link.xAttrKey)
            ?: decryptAndVerifyLinkXAttr(link, coroutineContext).getOrThrow()
    }

    suspend operator fun invoke(
        linkId: LinkId,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<DecryptedText> = getLink(linkId)
        .toResult()
        .mapCatching { link ->
            invoke(link, coroutineContext).getOrThrow()
        }

    private suspend fun decryptAndVerifyLinkXAttr(
        link: BaseLink,
        coroutineContext: CoroutineContext,
    ): Result<DecryptedText> = coRunCatching(coroutineContext) {
        getLinkKey(link.id).mapCatching { decryptKey ->
            val signatureAddress = when (link) {
                is File -> link.uploadedBy
                is Folder -> link.signatureEmail
                is Album -> link.signatureEmail
                else -> error("Link must be either file, folder or album and it was not")
            }
            val verificationKeys =
                getVerificationKeys(link.id, signatureAddress).getOrThrow().keyHolder
            unlockKey(decryptKey.keyHolder) { unlockedKey ->
                decryptAndVerifyText(
                    unlockedKey = unlockedKey,
                    text = requireNotNull(link.xAttr),
                    verifyKeyRing = getPublicKeyRing(verificationKeys).getOrThrow(),
                    verificationFailedContext = javaClass.simpleName,
                )
                    .onSuccess { decryptedXAttr ->
                        decryptedTextRepository.addDecryptedText(link.userId, link.xAttrKey, decryptedXAttr)
                    }
                    .getOrThrow()
            }.getOrThrow()
        }.getOrThrow()
    }

    private val BaseLink.xAttrKey: String
        get() = xAttrKey(
            sha256(requireNotNull(xAttr), Sha256.OutputFormat.BASE_64).getOrNull()
        )
}
