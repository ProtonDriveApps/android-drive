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
package me.proton.core.drive.crypto.domain.usecase.base

import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.exception.VerificationException
import me.proton.core.drive.cryptobase.domain.usecase.GetPublicKeyRing
import me.proton.core.drive.cryptobase.domain.usecase.UseSessionKey
import me.proton.core.drive.key.domain.entity.ContentKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UseSessionKey @Inject constructor(
    private val useSessionKey: UseSessionKey,
    private val getPublicKeyRing: GetPublicKeyRing,
) {
    suspend operator fun <T> invoke(
        contentKey: ContentKey,
        checkSignature: Boolean = false,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecryptWithIO.coroutineContext,
        block: suspend (SessionKey) -> T
    ) =
        useSessionKey(
            decryptKey = contentKey.decryptKey.keyHolder,
            verifyKeyRing = getPublicKeyRing(contentKey.verifyKey.keyHolder).getOrThrow(),
            encryptedKeyPacket = contentKey.encryptedKeyPacket,
            contentKeyPacketSignature = contentKey.contentKeyPacketSignature,
            coroutineContext = coroutineContext,
        ) { sessionKey, verified ->
            if (!verified) {
                CoreLogger.d(LogTag.ENCRYPTION, "Verification of session key failed")
                if (checkSignature) {
                    throw VerificationException("Verification of session key failed")
                }
            }
            block(sessionKey)
        }
}
