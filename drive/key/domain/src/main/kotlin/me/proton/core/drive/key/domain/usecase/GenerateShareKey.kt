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
import me.proton.core.drive.base.domain.usecase.GetAddressId
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.usecase.GenerateNestedPrivateKey
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.ShareKey
import me.proton.core.drive.key.domain.extension.keyHolder
import me.proton.core.user.domain.entity.AddressId
import javax.inject.Inject

/**
 * Generates new share [Key]
 */
class GenerateShareKey @Inject constructor(
    private val generateNestedPrivateKey: GenerateNestedPrivateKey,
    private val getAddressKeys: GetAddressKeys,
    private val getAddressId: GetAddressId,
) {
    suspend operator fun invoke(
        userId: UserId,
        addressId: AddressId,
    ): Result<Key.Node> = coRunCatching {
        val addressKey = getAddressKeys(userId, addressId).keyHolder
        ShareKey(
            key = generateNestedPrivateKey(
                userId = userId,
                encryptKey = addressKey,
                signKey = addressKey,
            ).getOrThrow()
        )
    }

    suspend operator fun invoke(userId: UserId): Result<Key.Node> = coRunCatching {
        invoke(
            userId = userId,
            addressId = getAddressId(userId),
        ).getOrThrow()
    }
}
