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
package me.proton.core.drive.key.domain.entity

import me.proton.core.drive.cryptobase.domain.entity.PublicAddressInfoKeyHolder
import me.proton.core.key.domain.entity.key.NestedPrivateKey
import me.proton.core.key.domain.entity.keyholder.KeyHolder

sealed interface Key {
    sealed interface Node: Key
}

internal data class ShareKey(val key: NestedPrivateKey) : Key.Node
internal data class NodeKey(val key: NestedPrivateKey, val parent: Key) : Key.Node
internal data class AddressKeys(val keyHolder: KeyHolder) : Key
internal data class PublicAddressKeys(val publicAddressInfoKeyHolder: PublicAddressInfoKeyHolder) : Key
