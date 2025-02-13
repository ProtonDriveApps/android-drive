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
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.entity.UnlockedKey
import me.proton.core.drive.cryptobase.domain.usecase.DecryptAndVerifyText
import me.proton.core.drive.cryptobase.domain.usecase.GetPublicKeyRing
import me.proton.core.drive.cryptobase.domain.usecase.UnlockKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetLinkParentKey
import me.proton.core.drive.key.domain.usecase.GetVerificationKeys
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.GetLink
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Link name is encrypted with parent node key so in order to decrypt it we first get node key from link parent and then
 * decrypt link name with it.
 */
class DecryptLinkName @Inject constructor(
    private val getLinkParentKey: GetLinkParentKey,
    private val unlockKey: UnlockKey,
    private val decryptAndVerifyText: DecryptAndVerifyText,
    private val getVerificationKeys: GetVerificationKeys,
    private val getLink: GetLink,
    private val getPublicKeyRing: GetPublicKeyRing,
) {
    suspend operator fun invoke(
        link: Link,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<DecryptedText> =
        getLinkParentKey(link).mapCatching { parentKey ->
            unlockKey(parentKey.keyHolder) { unlockedKey ->
                invoke(unlockedKey, link.id, coroutineContext).getOrThrow()
            }.getOrThrow()
        }

    suspend operator fun invoke(
        unlockedKey: UnlockedKey,
        linkId: LinkId,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<DecryptedText> = coRunCatching(coroutineContext) {
        val link = getLink(linkId).toResult().getOrThrow()
        val email = link.nameSignatureEmail ?: link.signatureEmail
        val verificationKeys = getVerificationKeys(
            link = link,
            email = email,
            fallbackTo = GetVerificationKeys.FallbackTo.PARENT_NODE_KEY
        ).getOrThrow().keyHolder
        decryptAndVerifyText(
            unlockedKey = unlockedKey,
            text = link.name,
            verifyKeyRing = getPublicKeyRing(verificationKeys).getOrThrow(),
            verificationFailedContext = javaClass.simpleName,
            coroutineContext = coroutineContext,
        ).getOrThrow()
    }
}
