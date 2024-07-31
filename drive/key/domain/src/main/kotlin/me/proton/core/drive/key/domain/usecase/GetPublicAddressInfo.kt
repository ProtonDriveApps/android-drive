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

package me.proton.core.drive.key.domain.usecase

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.api.ProtonApiCode.KEY_GET_ADDRESS_MISSING
import me.proton.core.drive.base.domain.api.ProtonApiCode.KEY_GET_DOMAIN_EXTERNAL
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.key.domain.repository.PublicAddressRepository
import me.proton.core.key.domain.repository.Source
import me.proton.core.network.domain.hasProtonErrorCode
import javax.inject.Inject

class GetPublicAddressInfo @Inject constructor(
    private val publicAddressRepository: PublicAddressRepository,
) {
    suspend operator fun invoke(
        userId: UserId,
        email: String,
        source: Source = Source.RemoteOrCached
    ) = coRunCatching {
        publicAddressRepository.getPublicAddressInfo(
            sessionUserId = userId,
            email = email,
            internalOnly = true,
            source = source,
        )
    }.map { publicAddressInfo ->
        publicAddressInfo.takeUnless { publicAddressInfo.address.keys.isEmpty() }
    }.recoverCatching { error ->
        if (error.hasProtonErrorCode(KEY_GET_ADDRESS_MISSING)
            || error.hasProtonErrorCode(KEY_GET_DOMAIN_EXTERNAL)
        ) {
            null
        } else {
            throw error
        }
    }
}
