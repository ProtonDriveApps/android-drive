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

package me.proton.core.drive.base.domain.function

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
class PagedListTest {
    private val items = (0..99).toList()

    private val paged = { index: Int, count: Int ->
        items.subList(index, min(items.size, index + count))
    }

    @Test
    fun bigger() = runTest {
        assertEquals(items, pagedList(200, paged))
    }

    @Test
    fun exact() = runTest {
        assertEquals(items, pagedList(items.size, paged))
    }

    @Test
    fun one_less_then_exact() = runTest {
        assertEquals(items, pagedList(items.size - 1, paged))
    }

    @Test
    fun half() = runTest {
        assertEquals(items, pagedList(50, paged))
    }

    @Test
    fun one() = runTest {
        assertEquals(items, pagedList(1, paged))
    }

    @Test(expected = IllegalArgumentException::class)
    fun zero() = runTest {
        pagedList(0, paged)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negative() = runTest {
        pagedList(-1, paged)
    }
}
