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
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.base.domain.util.coRunCatching
import me.proton.core.drive.cryptobase.domain.extension.keyHolder
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.entity.PublicAddressKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetPublicAddressKeys @Inject constructor(
    private val getPublicAddressInfo: GetPublicAddressInfo,
    private val getAddressKeys: GetAddressKeys,
) {

    suspend operator fun invoke(userId: UserId, email: String): Result<Key> = coRunCatching {
        check(email.isNotEmpty()) { "Cannot found key for anonymous user" }
        getAddressKey(userId, email)
            .getOrNull()
            ?: let {
                getPublicAddressInfo(userId, email)
                    .getOrNull(LogTag.KEY, "get public address info for email $email failed")
                    ?.let { publicAddressInfo ->
                        PublicAddressKeys(publicAddressInfo.keyHolder())
                    }
            }
            ?: getAddressKeys(userId, email)
    }

    private suspend fun getAddressKey(userId: UserId, email: String): Result<Key> = coRunCatching {
        getAddressKeys(
            userId = userId,
            email = email,
            fallbackToAllAddressKeys = false,
        )
    }
}
