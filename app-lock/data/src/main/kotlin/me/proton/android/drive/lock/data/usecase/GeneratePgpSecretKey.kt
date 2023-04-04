/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.proton.android.drive.lock.data.usecase

import me.proton.android.drive.lock.data.crypto.PgpSecretKey
import me.proton.android.drive.lock.domain.entity.SecretKey
import me.proton.android.drive.lock.domain.usecase.GenerateSecretKey
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.drive.base.domain.util.coRunCatching
import javax.inject.Inject

class GeneratePgpSecretKey @Inject constructor(
    private val cryptoContext: CryptoContext,
) : GenerateSecretKey {

    override suspend fun invoke(
        passphrase: ByteArray,
    ): Result<SecretKey> = coRunCatching {
        cryptoContext.pgpCrypto.generateRandomBytes()
        PgpSecretKey(
            passphrase = PlainByteArray(passphrase.clone()),
            lockedKey = cryptoContext.pgpCrypto.generateNewPrivateKey(DEFAULT_USERNAME, DEFAULT_DOMAIN, passphrase),
            cryptoContext = cryptoContext,
        )
    }

    companion object {
        private const val DEFAULT_USERNAME = "drive-app-lock-key"
        private const val DEFAULT_DOMAIN = "proton.me"
    }
}
