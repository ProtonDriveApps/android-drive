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
package me.proton.core.drive.cryptobase.domain.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.entity.keyholder.KeyHolder
import me.proton.core.key.domain.entity.keyholder.KeyHolderPrivateKey
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.repository.UserAddressRepository

suspend fun UserAddressRepository.getAddressKeys(
    userId: UserId,
    email: String,
    fallbackToAllAddressKeys: Boolean = true,
): KeyHolder =
    with (getAddresses(userId)) {
        firstOrNull { userAddress -> userAddress.email == email }?.keys?.keyHolder()
            ?: let {
                if (fallbackToAllAddressKeys) {
                    flatMap { userAddress -> userAddress.keys }.keyHolder()
                } else {
                    throw NoSuchElementException("Could not found user address for email: $email")
                }
            }
    }

suspend fun UserAddressRepository.getAddressKeys(
    userId: UserId,
    addressId: AddressId,
    fallbackToAllAddressKeys: Boolean = true,
): KeyHolder =
    with (getAddresses(userId)) {
        firstOrNull { userAddress -> userAddress.addressId == addressId }?.keys?.keyHolder()
            ?: let {
                if (fallbackToAllAddressKeys) {
                    flatMap { userAddress -> userAddress.keys }.keyHolder()
                } else {
                    throw NoSuchElementException("Could not found user address for address ID: ${addressId.id}")
                }
            }
    }

fun List<KeyHolderPrivateKey>.keyHolder() = object : KeyHolder {
    override val keys: List<KeyHolderPrivateKey> = this@keyHolder
}
