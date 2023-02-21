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
package me.proton.core.drive.key.domain.extension

import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.crypto.common.pgp.EncryptedMessage
import me.proton.core.crypto.common.pgp.Signature
import me.proton.core.drive.key.domain.entity.AddressKeys
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.NodeKey
import me.proton.core.drive.key.domain.entity.ShareKey
import me.proton.core.key.domain.entity.key.NestedPrivateKey
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.entity.keyholder.KeyHolderPrivateKey
import me.proton.core.key.domain.extension.keyHolder

val Key.Node.nodeKey: Armored
    get() = when(this) {
        is ShareKey -> key.privateKey.key
        is NodeKey -> key.privateKey.key
    }

val Key.Node.nodePassphrase: EncryptedMessage
    get() = requireNotNull(when(this) {
        is ShareKey -> key.passphrase
        is NodeKey -> key.passphrase
    }) { "Passphrase must not be null" }

val Key.Node.nodePassphraseSignature: Signature
    get() = requireNotNull(when(this) {
        is ShareKey -> key.passphraseSignature
        is NodeKey -> key.passphraseSignature
    }) { "PassphraseSignature must not be null" }

internal val Key.Node.nestedPrivateKey: NestedPrivateKey
    get() = when(this) {
        is ShareKey -> key
        is NodeKey -> key
    }

val Key.keyHolder: KeyHolder
    get() = when(this) {
        is ShareKey -> key.keyHolder()
        is NodeKey-> key.keyHolder()
        is AddressKeys -> keyHolder
    }

val List<Key>.keyHolder: KeyHolder get() = object : KeyHolder {
    override val keys: List<KeyHolderPrivateKey> get() = map { key: Key -> key.keyHolder.keys }.flatten()
}
