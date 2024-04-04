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

package me.proton.core.drive.base.data.repository

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.data.db.BaseDatabase
import me.proton.core.drive.base.data.db.entity.UrlLastFetchEntity
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.repository.BaseRepository
import javax.inject.Inject

class BaseRepositoryImpl @Inject constructor(
    private val db: BaseDatabase,
) :BaseRepository {

    override suspend fun getLastFetch(userId: UserId, url: String): TimestampMs? =
        db.urlLastFetchDao.get(userId, url)?.let(::TimestampMs)

    override suspend fun setLastFetch(userId: UserId, url: String, lastFetchTimestamp: TimestampMs) {
        db.urlLastFetchDao.insertOrUpdate(
            UrlLastFetchEntity(
                userId = userId,
                url = url,
                lastFetchTimestamp = lastFetchTimestamp.value,
            )
        )
    }
}
