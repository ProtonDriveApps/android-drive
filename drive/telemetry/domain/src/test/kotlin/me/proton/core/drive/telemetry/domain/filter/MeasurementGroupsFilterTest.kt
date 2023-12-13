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

package me.proton.core.drive.telemetry.domain.filter

import me.proton.core.domain.entity.UserId
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementGroupsFilterTest {
    private val filter = MeasurementGroupsFilter("a", "b", "c")

    private val userId = UserId("user-id")

    @Test
    fun `Given group in groups when filter should return true`() {
        assertTrue(filter(userId, DriveTelemetryEvent("a", "name")))
    }

    @Test
    fun `Given group not in groups when filter should return false`() {
        assertFalse(filter(userId, DriveTelemetryEvent("d", "name")))
    }
}
