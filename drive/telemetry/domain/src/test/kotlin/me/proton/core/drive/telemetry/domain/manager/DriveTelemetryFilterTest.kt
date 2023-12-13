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

package me.proton.core.drive.telemetry.domain.manager

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import me.proton.core.drive.telemetry.domain.extension.plus
import me.proton.core.drive.telemetry.domain.filter.MeasurementGroupsFilter
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DriveTelemetryFilterTest {
    private val userId = UserId("user-id")

    val interceptor = MeasurementGroupsFilter("group") + { _, event ->
        event.copy(dimensions = event.dimensions + ("dim" to "value"))
    }

    @Test
    fun notFiltered() = runTest {
        val event = interceptor(userId, DriveTelemetryEvent("group", "name"))

        assertEquals(
            DriveTelemetryEvent(
                group = "group",
                name = "name",
                dimensions = mapOf("dim" to "value")
            ),
            event,
        )
    }

    @Test
    fun filtered() = runTest {
        val originalEvent = DriveTelemetryEvent("other", "name")

        val event = interceptor(userId, originalEvent)

        assertEquals(originalEvent, event)
    }
}
