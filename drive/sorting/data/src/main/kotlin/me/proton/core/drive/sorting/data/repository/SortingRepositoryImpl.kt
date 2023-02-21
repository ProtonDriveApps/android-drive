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

package me.proton.core.drive.sorting.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.sorting.data.db.SortingDatabase
import me.proton.core.drive.sorting.data.db.entity.SortingEntity
import me.proton.core.drive.sorting.domain.entity.Sorting
import me.proton.core.drive.sorting.domain.repository.SortingRepository
import javax.inject.Inject

class SortingRepositoryImpl @Inject constructor(
    private val database: SortingDatabase,
) : SortingRepository {

    override fun getSorting(userId: UserId): Flow<Sorting> =
        database.sortingDao.getSorting(userId)
            .distinctUntilChanged()
            .map { entity ->
                entity?.let {
                    Sorting(entity.sortingBy, entity.sortingDirection)
                } ?: Sorting.DEFAULT
            }

    override suspend fun updateSorting(userId: UserId, sorting: Sorting) {
        database.sortingDao.insertOrUpdate(SortingEntity(userId, sorting.by, sorting.direction))
    }
}
