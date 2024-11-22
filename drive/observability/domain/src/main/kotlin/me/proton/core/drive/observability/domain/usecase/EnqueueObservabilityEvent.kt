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

package me.proton.core.drive.observability.domain.usecase

import me.proton.core.drive.observability.domain.constraint.Constraint
import me.proton.core.drive.observability.domain.constraint.NoConstraint
import me.proton.core.drive.observability.domain.metrics.DriveObservabilityData
import me.proton.core.observability.domain.ObservabilityManager
import me.proton.core.observability.domain.entity.ObservabilityEvent
import java.time.Instant
import javax.inject.Inject

@Suppress("NewApi")
class EnqueueObservabilityEvent @Inject constructor(
    private val manager: ObservabilityManager,
) {

    suspend operator fun invoke(
        observabilityData: DriveObservabilityData,
        timestamp: Instant = Instant.now(),
        constraint: Constraint = NoConstraint(),
    ) = runCatching {
        if (constraint.isMet()) {
            manager.enqueue(
                ObservabilityEvent(
                    data = observabilityData,
                    timestamp = timestamp,
                )
            )
            constraint.apply()
        }
    }
}
