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

package me.proton.core.drive.log.domain.repository

import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.log.domain.entity.Log

interface LogRepository {

    suspend fun insert(logs: List<Log>)

    suspend fun processAllLogs(userId: UserId, block: suspend (List<Log>) -> Unit)

    suspend fun toggleLogLevel(userId: UserId, level: Log.Level)

    fun getAllDeselectedLogLevels(userId: UserId, count: Int): Flow<Set<Log.Level>>

    suspend fun toggleLogOrigin(userId: UserId, origin: Log.Origin)

    fun getAllDeselectedLogOrigins(userId: UserId, count: Int): Flow<Set<Log.Origin>>
}
