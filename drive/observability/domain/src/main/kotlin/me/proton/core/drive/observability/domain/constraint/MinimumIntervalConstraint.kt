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

package me.proton.core.drive.observability.domain.constraint

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.extension.isOlderThen
import me.proton.core.drive.base.domain.repository.BaseRepository
import javax.inject.Inject
import kotlin.time.Duration


class MinimumIntervalConstraint @Inject constructor(
    private val repository: BaseRepository
) {

    operator fun invoke(userId: UserId, schemaId: String, interval: Duration): Constraint = object : Constraint {

        override suspend fun isMet(): Boolean =
            repository
                .getLastFetch(userId, schemaId)
                ?.isOlderThen(interval)
                ?: true

        override suspend fun apply() {
            require(isMet()) { "Apply should be called only when constraint is met" }
            repository.setLastFetch(userId, schemaId, TimestampMs())
        }
    }
}
