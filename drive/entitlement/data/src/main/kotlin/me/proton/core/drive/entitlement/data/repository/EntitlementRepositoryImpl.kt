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

package me.proton.core.drive.entitlement.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.entitlement.data.api.EntitlementApiDataSource
import me.proton.core.drive.entitlement.data.db.EntitlementDatabase
import me.proton.core.drive.entitlement.data.db.entity.EntitlementEntity
import me.proton.core.drive.entitlement.data.extension.fromString
import me.proton.core.drive.entitlement.domain.entity.Entitlement
import me.proton.core.drive.entitlement.domain.repository.EntitlementRepository
import javax.inject.Inject

class EntitlementRepositoryImpl @Inject constructor(
    private val api: EntitlementApiDataSource,
    private val db: EntitlementDatabase,
) : EntitlementRepository {

    override suspend fun fetchAndStoreEntitlements(userId: UserId) {

        val entitlements = api.getEntitlements(userId)
        val entitlementEntities =
            entitlements.map { (key, value) -> EntitlementEntity(userId, key, value) }
        db.inTransaction {
            db.entitlementDao.deleteAll(userId)
            db.entitlementDao.insertOrIgnore(*entitlementEntities.toTypedArray())
        }
    }

    override fun <T : Any> getEntitlement(
        userId: UserId,
        key: Entitlement.Key<T>
    ): Flow<Entitlement<T>?> =
        db.entitlementDao.getFlow(userId, key.name).map { entity ->
            entity?.let {
                key.fromString(entity.value)
            }
        }
}
