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
package me.proton.core.drive.key.data.factory

import android.util.Base64
import me.proton.core.crypto.common.pgp.KeyPacket
import me.proton.core.drive.key.domain.entity.ContentKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.factory.ContentKeyFactory
import javax.inject.Inject

class ContentKeyFactoryImpl @Inject constructor() : ContentKeyFactory {
    override fun createContentKey(
        decryptKey: Key.Node,
        verifyKey: List<Key>,
        encryptedKeyPacket: KeyPacket,
        contentKeyPacketSignature: String
    ): ContentKey = ContentKeyImpl(
        decryptKey = decryptKey,
        verifyKey = verifyKey,
        encryptedKeyPacket = encryptedKeyPacket,
        contentKeyPacket = Base64.encodeToString(encryptedKeyPacket, Base64.NO_WRAP),
        contentKeyPacketSignature = contentKeyPacketSignature,
    )

    override fun createContentKey(
        decryptKey: Key.Node,
        verifyKey: List<Key>,
        contentKeyPacket: String,
        contentKeyPacketSignature: String
    ): ContentKey = ContentKeyImpl(
        decryptKey = decryptKey,
        verifyKey = verifyKey,
        encryptedKeyPacket = Base64.decode(contentKeyPacket, Base64.NO_WRAP),
        contentKeyPacket = contentKeyPacket,
        contentKeyPacketSignature = contentKeyPacketSignature,
    )
}

internal data class ContentKeyImpl constructor(
    override val decryptKey: Key.Node,
    override val verifyKey: List<Key>,
    override val encryptedKeyPacket: KeyPacket,
    override val contentKeyPacket: String,
    override val contentKeyPacketSignature: String
) : ContentKey {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContentKeyImpl

        if (decryptKey != other.decryptKey) return false
        if (verifyKey != other.verifyKey) return false
        if (!encryptedKeyPacket.contentEquals(other.encryptedKeyPacket)) return false
        if (contentKeyPacket != other.contentKeyPacket) return false
        if (contentKeyPacketSignature != other.contentKeyPacketSignature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = decryptKey.hashCode()
        result = 31 * result + verifyKey.hashCode()
        result = 31 * result + encryptedKeyPacket.contentHashCode()
        result = 31 * result + contentKeyPacket.hashCode()
        result = 31 * result + contentKeyPacketSignature.hashCode()
        return result
    }
}
