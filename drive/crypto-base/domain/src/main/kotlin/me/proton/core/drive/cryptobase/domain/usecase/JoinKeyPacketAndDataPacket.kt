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

import me.proton.core.crypto.common.pgp.DataPacket
import me.proton.core.crypto.common.pgp.EncryptedMessage
import me.proton.core.crypto.common.pgp.KeyPacket
import me.proton.core.crypto.common.pgp.Unarmored
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.CryptoScope
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class JoinKeyPacketAndDataPacket @Inject constructor(
    private val getUnarmored: GetUnarmored,
) {

    operator fun invoke(
        keyPacket: KeyPacket,
        dataPacket: DataPacket,
    ): Result<Unarmored> = Result.success(keyPacket + dataPacket)

    suspend operator fun invoke(
        keyPacket: EncryptedMessage,
        dataPacket: EncryptedMessage,
        coroutineContext: CoroutineContext = CryptoScope.EncryptAndDecrypt.coroutineContext,
    ): Result<Unarmored> = coRunCatching(coroutineContext) {
        invoke(
            keyPacket = getUnarmored(keyPacket, coroutineContext).getOrThrow(),
            dataPacket = getUnarmored(dataPacket, coroutineContext).getOrThrow(),
        ).getOrThrow()
    }
}
