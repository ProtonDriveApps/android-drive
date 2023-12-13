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
package me.proton.core.drive.crypto.domain.usecase.upload

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.SignData
import me.proton.core.drive.crypto.domain.usecase.file.GetManifest
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.linkupload.domain.entity.UploadBlock
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ManifestSignature @Inject constructor(
    private val getManifest: GetManifest,
    private val signData: SignData,
) {
    suspend operator fun invoke(
        signKey: Key,
        input: List<UploadBlock>,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<String> = coRunCatching(coroutineContext) {
        signData(
            signKey = signKey,
            input = getManifest(input).getOrThrow(),
        ).getOrThrow()
    }

    suspend operator fun invoke(
        signKey: Key,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<String> = coRunCatching(coroutineContext) {
        signData(
            signKey = signKey,
            input = ByteArray(0),
        ).getOrThrow()
    }
}
