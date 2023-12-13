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

package me.proton.core.drive.telemetry.domain.interceptor

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.usecase.GetUser
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanDimensionInterceptorTest {
    private val user = mockk<User>()
    private val getUser = mockk<GetUser>()
    private val interceptor = PlanDimensionInterceptor(getUser)

    private val userId = UserId("user-id")

    @Test
    fun free() = runTest {
        coEvery { getUser(userId, false) } returns user
        every { user.subscribed } returns 0

        val event = interceptor(userId, DriveTelemetryEvent("group", "name"))

        assertEquals(
            DriveTelemetryEvent(
                group = "group",
                name = "name",
                dimensions = mapOf("plan" to "free"),
            ),
            event,
        )
    }

    @Test
    fun paid() = runTest {
        coEvery { getUser(userId, false) } returns user
        every { user.subscribed } returns 1

        val event = interceptor(userId, DriveTelemetryEvent("group", "name"))

        assertEquals(
            DriveTelemetryEvent(
                group = "group",
                name = "name",
                dimensions = mapOf("plan" to "paid"),
            ),
            event,
        )
    }

    @Test
    fun error() = runTest {
        coEvery { getUser(userId, false) } throws IllegalStateException()

        val event = interceptor(userId, DriveTelemetryEvent("group", "name"))

        assertEquals(
            DriveTelemetryEvent(
                group = "group",
                name = "name",
                dimensions = mapOf("plan" to ""),
            ),
            event,
        )
    }
}
