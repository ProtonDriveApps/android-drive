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
package me.proton.core.drive.cryptobase.domain.usecase

import me.proton.core.crypto.common.pgp.DecryptedText
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.entity.UnlockedKey
import me.proton.core.drive.cryptobase.domain.exception.VerificationException
import me.proton.core.drive.cryptobase.domain.extension.withUnlockedKey
import me.proton.core.key.domain.decryptAndVerifyText
import me.proton.core.key.domain.entity.key.PublicKeyRing
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DecryptAndVerifyText @Inject constructor() {

    suspend operator fun invoke(
        unlockedKey: UnlockedKey,
        text: String,
        verifyKeyRing: PublicKeyRing,
        verificationFailedContext: String = "",
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<DecryptedText> = coRunCatching(coroutineContext) {
        withUnlockedKey(unlockedKey) { key ->
            key.decryptAndVerifyText(text, verifyKeyRing)
        }.also { decryptedText ->
            if (decryptedText.status != VerificationStatus.Success) {
                CoreLogger.w(
                    tag = LogTag.ENCRYPTION,
                    e = VerificationException(
                        message = "Verification status ${decryptedText.status.name} ($verificationFailedContext)"
                    ),
                    message = "Verification failed ($verificationFailedContext)"
                )
            }
        }
    }
}
