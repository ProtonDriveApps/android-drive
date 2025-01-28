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

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.exception.VerificationException
import me.proton.core.drive.cryptobase.domain.extension.failed
import me.proton.core.drive.cryptobase.domain.extension.toDecryptedFile
import me.proton.core.key.domain.entity.key.PublicKeyRing
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.useKeys
import me.proton.core.key.domain.verifyFileEncrypted
import me.proton.core.util.kotlin.CoreLogger
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class VerifyFileSignature @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val announceEvent: AnnounceEvent,
) {
    suspend operator fun invoke(
        decryptKey: KeyHolder,
        verifyKeyRing: PublicKeyRing,
        file: File,
        encSignature: String?,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecryptWithIO.coroutineContext,
    ): Result<VerificationStatus> = coRunCatching(coroutineContext) {
        when {
            encSignature == null -> VerificationStatus.NotSigned
            decryptKey.useKeys(cryptoContext) {
                verifyFileEncrypted(file.toDecryptedFile().file, encSignature, verifyKeyRing).also { verified ->
                    if (!verified) {
                        announceEvent(Event.SignatureVerificationFailed(verifyKeyRing.keys))
                    }
                }
            } -> VerificationStatus.Success
            else -> VerificationStatus.Failure
        }.also { status ->
            if (status.failed) {
                CoreLogger.w(
                    tag = LogTag.ENCRYPTION,
                    e = VerificationException("Verification status ${status.name}"),
                    message = "Verification of file failed"
                )
            }
        }
    }
}
