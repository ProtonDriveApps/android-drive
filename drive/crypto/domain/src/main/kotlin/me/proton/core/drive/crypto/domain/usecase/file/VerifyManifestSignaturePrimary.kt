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
package me.proton.core.drive.crypto.domain.usecase.file

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import me.proton.core.drive.cryptobase.domain.usecase.GetPublicKeyRing
import me.proton.core.drive.cryptobase.domain.usecase.VerifyData
import me.proton.core.drive.file.base.domain.entity.Block
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetAddressKeys
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class VerifyManifestSignaturePrimary @Inject constructor(
    private val getAddressKeys: GetAddressKeys,
    private val verifyData: VerifyData,
    private val getManifest: GetManifest,
    private val getPublicKeyRing: GetPublicKeyRing,
) {
    suspend operator fun invoke(
        userId: UserId,
        signatureAddress: String,
        blocks: List<Block>,
        manifestSignature: String,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<Boolean> = coRunCatching(coroutineContext) {
        verifyData(
            publicKey = getPublicKeyRing(
                getAddressKeys(
                    userId = userId,
                    email = signatureAddress
                ).keyHolder
            ).getOrThrow().primaryKey,
            input = getManifest(blocks).getOrThrow(),
            signature = manifestSignature,
        ).getOrThrow()
    }
}
