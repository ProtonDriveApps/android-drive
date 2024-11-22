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

package me.proton.core.drive.observability.data.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.observability.data.db.ObservabilityDatabase
import me.proton.core.drive.observability.data.db.entity.CounterEntity
import me.proton.core.drive.observability.domain.repository.CounterRepository
import javax.inject.Inject

class CounterRepositoryImpl @Inject constructor(
    private val db: ObservabilityDatabase,
) : CounterRepository {

    override suspend fun get(userId: UserId, key: String): Int? =
        db.counterDao.get(userId, key)?.count

    override suspend fun increase(userId: UserId, key: String) =
        db.inTransaction {
            val counter = db.counterDao.get(userId, key)
            db.counterDao.insertOrUpdate(
                counter?.copy(
                    count = counter.count + 1,
                    lastModified = TimestampMs().value
                )
                    ?: CounterEntity(
                        userId = userId,
                        key = key,
                        count = 1,
                        lastModified = TimestampMs().value,
                    )
            )
        }

    override suspend fun deleteAllOlderThen(userId: UserId, timestamp: TimestampMs) =
        db.counterDao.deleteAllOlderThen(userId, timestamp.value)
}
