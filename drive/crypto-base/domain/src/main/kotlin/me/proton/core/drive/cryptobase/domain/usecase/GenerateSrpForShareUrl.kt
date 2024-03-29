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

import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.entity.SrpForShareUrl
import me.proton.core.network.domain.session.SessionProvider
import javax.inject.Inject

class GenerateSrpForShareUrl @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val authRepository: AuthRepository,
    private val sessionProvider: SessionProvider,
) {
    suspend operator fun invoke(userId: UserId, urlPassword: ByteArray) = coRunCatching {
        val sessionId = sessionProvider.getSessionId(userId)
        val modulus = authRepository.randomModulus(sessionId)
        val auth = cryptoContext.srpCrypto.calculatePasswordVerifier(
            username = "",
            password = urlPassword,
            modulusId = modulus.modulusId,
            modulus = modulus.modulus,
        )
        SrpForShareUrl(
            verifier = auth.verifier,
            modulusId = auth.modulusId,
            urlPasswordSalt = auth.salt,
        )
    }
}
