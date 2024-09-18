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
package me.proton.core.drive.cryptobase.domain.usecase

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.key.domain.entity.key.PublicKey
import me.proton.core.key.domain.entity.key.PublicKeyRing
import me.proton.core.key.domain.verifyData
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class VerifyData @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val announceEvent: AnnounceEvent,
) {
    suspend operator fun invoke(
        verifyKeyRing: PublicKeyRing,
        input: ByteArray,
        signature: String,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ) = coRunCatching(coroutineContext) {
        verifyKeyRing.verifyData(cryptoContext, input, signature).also { verified ->
            if (!verified) {
                announceEvent(Event.SignatureVerificationFailed(verifyKeyRing.keys))
            }
        }
    }

    suspend operator fun invoke(
        publicKey: PublicKey,
        input: ByteArray,
        signature: String,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ) = coRunCatching(coroutineContext) {
        publicKey.verifyData(cryptoContext, input, signature).also { verified ->
            if (!verified) {
                announceEvent(Event.SignatureVerificationFailed(listOf(publicKey)))
            }
        }
    }
}
