/*
 * Copyright (c) 2023 Proton AG.
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

package me.proton.core.drive.telemetry.data.manager

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.telemetry.data.extension.toTelemetryEvent
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import me.proton.core.drive.telemetry.domain.manager.DriveTelemetryInterceptor
import me.proton.core.drive.telemetry.domain.manager.DriveTelemetryManager
import me.proton.core.telemetry.domain.TelemetryManager
import javax.inject.Inject

class DriveTelemetryManagerImpl @Inject constructor(
    private val manager: TelemetryManager,
) : DriveTelemetryManager {

    private var interceptors: List<DriveTelemetryInterceptor> = emptyList()

    override suspend fun enqueue(userId: UserId, event: DriveTelemetryEvent) {
        val eventToEnqueue = interceptors.fold(event) { previousEvent, interceptor ->
            interceptor(userId, previousEvent)
        }
        manager.enqueue(userId, eventToEnqueue.toTelemetryEvent())
    }

    override fun addInterceptor(interceptor: DriveTelemetryInterceptor) {
        interceptors += interceptor
    }
}
