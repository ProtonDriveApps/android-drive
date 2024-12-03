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

package me.proton.core.drive.base.domain.extension

import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.User
import org.junit.Assert.assertEquals
import org.junit.Test

class UserAvailableSpaceTest {

    @Test
    fun zero() {
        val user = user(
            maxSpace = 0,
            usedSpace = 0,
        )

        assertEquals(0.bytes, user.availableSpace)
    }

    @Test
    fun empty() {
        val user = user(
            maxSpace = 100,
            usedSpace = 0,
        )

        assertEquals(100.bytes, user.availableSpace)
    }

    @Test
    fun `mostly used`() {
        val user = user(
            maxSpace = 100,
            usedSpace = 75,
        )

        assertEquals(25.bytes, user.availableSpace)
    }

    @Test
    fun `fully used`() {
        val user = user(
            maxSpace = 100,
            usedSpace = 100,
        )

        assertEquals(0.bytes, user.availableSpace)
    }

    @Test
    fun `over used`() {
        val user = user(
            maxSpace = 100,
            usedSpace = 125,
        )

        assertEquals(0.bytes, user.availableSpace)
    }

    private fun user(maxSpace: Long, usedSpace: Long) = User(
        userId = UserId("id"),
        email = "",
        name = "",
        displayName = "",
        currency = "",
        credit = 0,
        usedSpace = usedSpace,
        maxSpace = maxSpace,
        maxUpload = 0,
        role = null,
        private = false,
        services = 0,
        subscribed = 0,
        delinquent = null,
        keys = emptyList(),
        recovery = null,
        createdAtUtc = 0,
        type = null,
        flags = emptyMap(),
    )
}
