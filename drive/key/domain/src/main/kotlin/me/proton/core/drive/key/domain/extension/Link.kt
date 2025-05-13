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

import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.key.domain.entity.key.NestedPrivateKey

internal val Link.keyId: String
    get() = id.id + id.shareId + passphrase

internal val Link.nestedPrivateKey
    get() = NestedPrivateKey.from(key, passphrase, passphraseSignature)

fun Link.signatureEmail(
    signatureAddress: String
) = if (signatureEmail.isEmpty()) {
    signatureAddress
} else {
    null
}

fun Link.nodePassphraseSignature(
    nodeKey: Key.Node,
) = if (signatureEmail.isEmpty() || nameSignatureEmail.isNullOrEmpty()) {
    nodeKey.nodePassphraseSignature
} else {
    null
}
