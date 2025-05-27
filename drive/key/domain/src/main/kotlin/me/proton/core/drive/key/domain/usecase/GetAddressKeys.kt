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

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.cryptobase.domain.extension.getAddressKeys
import me.proton.core.drive.key.domain.entity.AddressKeys
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.repository.UserAddressRepository
import javax.inject.Inject

class GetAddressKeys @Inject constructor(
    private val userAddressRepository: UserAddressRepository,
) {
    suspend operator fun invoke(
        userId: UserId,
        addressId: AddressId,
        fallbackToAllAddressKeys: Boolean = true,
    ): Key =
        AddressKeys(
            userAddressRepository.getAddressKeys(
                userId = userId,
                addressId = addressId,
                fallbackToAllAddressKeys = fallbackToAllAddressKeys,
            )
        )

    suspend operator fun invoke(
        userId: UserId,
        email: String,
        fallbackToAllAddressKeys: Boolean = true,
        isUsedForSignatureVerification: Boolean = false,
    ): Key =
        AddressKeys(
            userAddressRepository.getAddressKeys(
                userId = userId,
                email = email,
                fallbackToAllAddressKeys = fallbackToAllAddressKeys,
                isUsedForSignatureVerification = isUsedForSignatureVerification,
            )
        )
}
