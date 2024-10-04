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
package me.proton.core.drive.base.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag.KEY
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.user.domain.extension.primary
import me.proton.core.user.domain.extension.sorted
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class GetUserEmail @Inject constructor(
    private val userManager: UserManager
) {
    suspend operator fun invoke(userId: UserId): String =
        userManager.getUser(userId).email
            ?: userManager.getAddresses(userId).primary().let { userAddress ->
                if (userAddress == null) {
                    CoreLogger.e(
                        KEY,
                        IllegalStateException("Primary address not found"),
                        "Primary address not found for $userId"
                    )
                    userManager.getAddresses(userId).sorted().first()
                } else {
                    userAddress
                }.email
            }

    suspend operator fun invoke(userId: UserId, addressId: AddressId): String =
        userManager.getAddresses(userId)
            .first { address -> address.addressId == addressId }
            .email
}
