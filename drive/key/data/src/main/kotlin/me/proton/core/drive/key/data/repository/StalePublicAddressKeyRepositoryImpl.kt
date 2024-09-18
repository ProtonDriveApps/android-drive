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

package me.proton.core.drive.key.data.repository

import me.proton.core.crypto.common.pgp.Armored
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.key.data.db.PublicAddressKeyDatabase
import me.proton.core.drive.key.data.db.entity.StalePublicAddressKeyEntity
import me.proton.core.drive.key.domain.repository.StalePublicAddressKeyRepository
import javax.inject.Inject

class StalePublicAddressKeyRepositoryImpl @Inject constructor(
    private val db: PublicAddressKeyDatabase
) : StalePublicAddressKeyRepository {

    override val publicAddressInfoUrl: String
        get() = CORE_API_PUBLIC_ADDRESS_INFO_URL

    override suspend fun markAsStale(userId: UserId, email: String, key: Armored) =
        db.stalePublicAddressKeyDao.insertOrUpdate(
            StalePublicAddressKeyEntity(
                userId = userId,
                email = email,
                key = key,
            )
        )

    override suspend fun removeAllStale(userId: UserId, email: String) =
        db.stalePublicAddressKeyDao.deleteAll(userId, email)

    override suspend fun hasStale(userId: UserId, email: String): Boolean =
        db.stalePublicAddressKeyDao.hasEmail(userId, email)

    override suspend fun getEmails(userId: UserId, key: Armored): List<String> =
        db.publicAddressKeyDataDao.getEmail(key)

    companion object {
        private const val CORE_API_PUBLIC_ADDRESS_INFO_URL = "/core/v4/keys/all"
    }
}
