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
package me.proton.core.drive.crypto.data.usecase.base

import android.util.Base64
import me.proton.core.crypto.common.pgp.EncryptedMessage
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.crypto.domain.usecase.base.ReencryptKeyPacket
import me.proton.core.drive.cryptobase.domain.usecase.EncryptSessionKey
import me.proton.core.drive.cryptobase.domain.usecase.GetSessionKeyFromEncryptedMessage
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.drive.key.domain.usecase.GetLinkParentKey
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.link.domain.entity.LinkId
import me.proton.core.drive.link.domain.usecase.GetLink
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import javax.inject.Inject

class ReencryptKeyPacketImpl @Inject constructor(
    private val getLinkParentKey: GetLinkParentKey,
    private val getSessionKeyFromEncryptedMessage: GetSessionKeyFromEncryptedMessage,
    private val encryptSessionKey: EncryptSessionKey,
    private val getLink: GetLink,
) : ReencryptKeyPacket {

    override suspend operator fun invoke(
        message: EncryptedMessage,
        link: Link,
        shareKey: Key.Node,
    ): Result<String> = coRunCatching {
        Base64.encodeToString(
            reencryptKeyPacket(
                decryptKey = getLinkParentKey(link).getOrThrow().keyHolder,
                message = message,
                encryptKey = shareKey.keyHolder,
            ).getOrThrow(),
            Base64.NO_WRAP,
        )
    }

    override suspend fun invoke(
        message: EncryptedMessage,
        linkId: LinkId,
        shareKey: Key.Node
    ): Result<String> = coRunCatching {
        invoke(
            message = message,
            link = getLink(linkId).toResult().getOrThrow(),
            shareKey = shareKey,
        ).getOrThrow()
    }

    private suspend fun reencryptKeyPacket(
        decryptKey: KeyHolder,
        message: EncryptedMessage,
        encryptKey: KeyHolder,
    ): Result<ByteArray> = coRunCatching {
        val sessionKey = getSessionKeyFromEncryptedMessage(
            decryptKey = decryptKey,
            message = message,
        ).getOrThrow()
        encryptSessionKey(
            encryptKey = encryptKey,
            sessionKey = sessionKey,
        ).getOrThrow()
    }
}
