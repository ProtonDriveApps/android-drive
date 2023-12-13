/*
 * Copyright (c) 2023 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.telemetry

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import me.proton.core.drive.telemetry.domain.extension.zeroTimestamp
import me.proton.core.drive.telemetry.domain.manager.DriveTelemetryInterceptor
import me.proton.core.drive.telemetry.domain.manager.DriveTelemetryManager

class StubbedDriveTelemetryManager : DriveTelemetryManager {

    var events = mutableMapOf<UserId, List<DriveTelemetryEvent>>()
    override suspend fun enqueue(userId: UserId, event: DriveTelemetryEvent) {
        events[userId] = events.getOrDefault(userId, emptyList()) + event.zeroTimestamp()
    }

    override fun addInterceptor(interceptor: DriveTelemetryInterceptor) {
        TODO("Not yet implemented")
    }
}
