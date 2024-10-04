/*
 * Copyright (c) 2024 Proton AG.
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

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.decrypt
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.entity.CipherSpec
import me.proton.core.drive.crypto.domain.entity.SessionForkPayload
import me.proton.core.drive.crypto.domain.extension.asJson
import me.proton.core.user.domain.repository.PassphraseRepository
import javax.inject.Inject

class GetSessionForkPayload @Inject constructor(
    private val passphraseRepository: PassphraseRepository,
    private val cryptoContext: CryptoContext,
    private val encryptData: EncryptData,
) {
    suspend operator fun invoke(userId: UserId, sessionKey: SessionKey): Result<String> = coRunCatching {
        encryptData(
            encryptKey = sessionKey,
            cipherSpec = CipherSpec.AES_GCM_NO_PADDING_IV_16_BYTES,
            input = createSessionForkPayload(userId).getOrThrow().asJson.toByteArray(),
        ).getOrThrow()
    }

    private suspend fun createSessionForkPayload(userId: UserId): Result<SessionForkPayload> = coRunCatching {
        requireNotNull(passphraseRepository.getPassphrase(userId)) { "Getting user passphrase failed" }
            .decrypt(cryptoContext.keyStoreCrypto).use { plainByteArray ->
                SessionForkPayload(
                    keyPassword = String(plainByteArray.array)
                )
            }
    }
}
