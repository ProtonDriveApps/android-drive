/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.telemetry.domain.interceptor

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.log.LogTag.TELEMETRY
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import me.proton.core.drive.telemetry.domain.manager.DriveTelemetryInterceptor
import me.proton.core.user.domain.extension.hasSubscription
import me.proton.core.user.domain.usecase.GetUser
import me.proton.core.util.kotlin.CoreLogger
import javax.inject.Inject

class PlanDimensionInterceptor @Inject constructor(
    private val getUser: GetUser,
) : DriveTelemetryInterceptor {
    override suspend fun invoke(
        userId: UserId,
        event: DriveTelemetryEvent,
    ): DriveTelemetryEvent = runCatching {
        getUser(userId, false)
    }.fold(
        onSuccess = { user ->
            if (user.hasSubscription()) {
                "paid"
            } else {
                "free"
            }
        },
        onFailure = { error ->
            CoreLogger.w(TELEMETRY, error, "Cannot get user for plan dimension")
            ""
        }
    ).let { plan ->
        val dimension = "plan" to plan
        event.copy(dimensions = event.dimensions + dimension)
    }

}
