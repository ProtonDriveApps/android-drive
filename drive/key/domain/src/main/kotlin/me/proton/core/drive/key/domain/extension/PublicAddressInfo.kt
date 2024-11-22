/*
 * Copyright (c) 2024 Proton AG.
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

import me.proton.core.key.domain.entity.key.PublicAddressInfo
import me.proton.core.key.domain.entity.key.PublicAddressKey

fun PublicAddressInfo.hasKeys(
    unverified: Boolean = false
) = address.keys.isNotEmpty() || (unverified && this.unverified?.keys.orEmpty().isNotEmpty())


fun PublicAddressInfo.primaryPublicKey(
    unverified: Boolean = false
): PublicAddressKey = address.keys.firstOrNull { publicAddressKey ->
    publicAddressKey.publicKey.isPrimary
} ?: if (unverified) {
    this.unverified?.keys.orEmpty().firstOrNull { publicAddressKey ->
        publicAddressKey.publicKey.isPrimary
    } ?: error("No primary key found: keys(${address.keys.size}), unverified keys(${this.unverified?.keys?.size})")
} else {
    error("No primary key found: keys(${address.keys.size})")
}
