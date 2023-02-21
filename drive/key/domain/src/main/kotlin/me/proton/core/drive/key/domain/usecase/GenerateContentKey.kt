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
package me.proton.core.drive.key.domain.usecase

import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.GenerateKeyPacket
import me.proton.core.drive.cryptobase.domain.usecase.SignData
import me.proton.core.drive.cryptobase.domain.usecase.UseSessionKey
import me.proton.core.drive.key.domain.entity.ContentKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.factory.ContentKeyFactory
import javax.inject.Inject

class GenerateContentKey @Inject constructor(
    private val generateKeyPacket: GenerateKeyPacket,
    private val signData: SignData,
    private val contentKeyFactory: ContentKeyFactory,
    private val useSessionKey: UseSessionKey,
) {
    suspend operator fun invoke(decryptKey: Key.Node): Result<ContentKey> = coRunCatching {
        val encryptedKeyPacket = generateKeyPacket(decryptKey.keyHolder).getOrThrow()
        val contentKeyPacketSignature = useSessionKey(
            decryptKey = decryptKey.keyHolder,
            encryptedKeyPacket = encryptedKeyPacket,
        ) { sessionKey ->
            signData(decryptKey.keyHolder, sessionKey.key).getOrThrow()
        }
        contentKeyFactory.createContentKey(
            decryptKey = decryptKey,
            verifyKey = listOf(decryptKey),
            encryptedKeyPacket = encryptedKeyPacket,
            contentKeyPacketSignature = contentKeyPacketSignature.getOrThrow(),
        )
    }
}
