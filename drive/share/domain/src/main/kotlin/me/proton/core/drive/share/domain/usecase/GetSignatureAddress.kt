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

package me.proton.core.drive.share.domain.usecase

import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag.KEY
import me.proton.core.drive.base.domain.usecase.GetSignatureAddress
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.share.domain.entity.ShareId
import javax.inject.Inject

class GetSignatureAddress @Inject constructor(
    private val getAddressId: GetAddressId,
    private val getSignatureAddress: GetSignatureAddress,
) {
    suspend operator fun invoke(
        shareId: ShareId,
    ): Result<String> = coRunCatching {
        getAddressId(shareId)
            .getOrNull(KEY, "No address id found for $shareId, fallback to primary address")
            ?.let { addressId ->
                getSignatureAddress(
                    userId = shareId.userId,
                    addressId = addressId,
                )
            } ?: getSignatureAddress(shareId.userId)
    }
}
