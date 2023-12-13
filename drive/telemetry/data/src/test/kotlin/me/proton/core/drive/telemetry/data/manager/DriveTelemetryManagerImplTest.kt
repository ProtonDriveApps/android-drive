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

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryEvent
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DriveTelemetryManagerImplTest {
    private val telemetryManager = mockk<TelemetryManager>(relaxed = true)

    private val driveTelemetryManager = DriveTelemetryManagerImpl(telemetryManager)
    private val userId = UserId("user-id")

    @Test
    fun enqueue() = runTest {
        driveTelemetryManager.enqueue(
            userId, DriveTelemetryEvent(
                group = "group",
                name = "name",
                timestamp = TimestampS(0),
            )
        )

        coVerify {
            telemetryManager.enqueue(
                userId, TelemetryEvent(
                    group = "group",
                    name = "name",
                    timestamp = 0,
                )
            )
        }
    }

    @Test
    fun interceptor() = runTest {
        driveTelemetryManager.addInterceptor { _, event ->
            event.copy(dimensions = event.dimensions + ("dim" to "value"))
        }

        driveTelemetryManager.enqueue(
            userId, DriveTelemetryEvent(
                group = "group",
                name = "name",
                timestamp = TimestampS(0),
            )
        )

        coVerify {
            telemetryManager.enqueue(
                userId, TelemetryEvent(
                    group = "group",
                    name = "name",
                    dimensions = mapOf("dim" to "value"),
                    timestamp = 0,
                )
            )
        }
    }
}
