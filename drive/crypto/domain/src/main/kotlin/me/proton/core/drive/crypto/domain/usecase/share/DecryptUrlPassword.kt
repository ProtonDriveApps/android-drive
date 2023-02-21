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
package me.proton.core.drive.crypto.domain.usecase.share

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.usecase.DecryptData
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class DecryptUrlPassword @Inject constructor(
    private val getAddressKeys: GetAddressKeys,
    private val decryptData: DecryptData,
) {
    suspend operator fun invoke(
        userId: UserId,
        encryptedUrlPassword: String,
        creatorEmail: String,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<String> = coRunCatching(coroutineContext) {
        decryptData(
            decryptKey = getAddressKeys(userId, creatorEmail).keyHolder,
            data = encryptedUrlPassword,
            coroutineContext = coroutineContext,
        ).getOrThrow().toString(Charsets.UTF_8)
    }
}
