/*
 * Copyright (c) 2025 Proton AG.
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

package me.proton.core.drive.key.domain.usecase

import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.keystore.encrypt
import me.proton.core.crypto.common.keystore.use
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.usecase.DecryptData
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.ShareKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.key.domain.entity.key.NestedPrivateKey
import me.proton.core.key.domain.entity.key.PrivateKey
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class BuildSharePrivateKey @Inject constructor(
    private val getAddressKeys: GetAddressKeys,
    private val decryptData: DecryptData,
    private val cryptoContext: CryptoContext,
) {
    suspend operator fun invoke(
        userId: UserId,
        email: String,
        shareKey: String,
        passphrase: String,
    ): Result<Key.Node> = coRunCatching {
        decryptData(
            decryptKey = getAddressKeys(
                userId = userId,
                email = email,
            ).keyHolder,
            data = passphrase,
        ).getOrThrow().use { decryptedPassphrase ->
            ShareKey(
                key = NestedPrivateKey(
                    privateKey = PrivateKey(
                        key = shareKey,
                        isPrimary = true,
                        passphrase = decryptedPassphrase.encrypt(cryptoContext.keyStoreCrypto)
                    ),
                    passphrase = null,
                    passphraseSignature = null,
                )
            )
        }
    }
}
