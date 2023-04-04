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
package me.proton.core.drive.base.data.test.manager

import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StubbedWorkManagerTest {
    private val manager = StubbedWorkManager()

    @Test
    fun `Given success behavior When add work without data Then returns success and succeed asserting`() {
        manager.behavior = StubbedWorkManager.BEHAVIOR_SUCCESS

        val result = manager.add("name")

        assertEquals(DataResult.Success(ResponseSource.Local, ""), result)
        manager.assertHasWorks("name")
    }

    @Test
    fun `Given success behavior When add work with data Then returns success and succeed asserting`() {
        manager.behavior = StubbedWorkManager.BEHAVIOR_SUCCESS

        val result = manager.add("name", "data")

        assertEquals(DataResult.Success(ResponseSource.Local, ""), result)
        manager.assertHasWork("name", "data")
    }

    @Test
    fun `Given error behavior When add work Then returns error and fail asserting`() {
        manager.behavior = StubbedWorkManager.BEHAVIOR_ERROR

        val result = manager.add("name")

        assertEquals(DataResult.Error.Local("behavior_error", null), result)
        assertThrows(AssertionError::class.java) {
            manager.assertHasWorks("name")
        }
    }

    @Test
    fun `Given a work When execute Then have no work`() {
        manager.add("name")

        manager.execute()

        assertEquals(emptyList<StubbedWorkManager.Work>(), manager.works.value)
    }

    @Test
    fun `Given no work When asserting for works Then fail`() {
        val exception = assertThrows(AssertionError::class.java) {
            manager.assertHasWorks("name")
        }
        assertEquals(
            "Does not contains work: name in []",
            exception.message
        )
    }

    @Test
    fun `Given work with different name When asserting for work Then fail`() {
        manager.add("different-name")

        val exception = assertThrows(AssertionError::class.java) {
            manager.assertHasWork("name")
        }

        assertEquals(
            "Does not contains work: name in [different-name]",
            exception.message
        )
    }

    @Test
    fun `Given two same works When asserting for one work Then fail`() {
        manager.add("name", "data")
        manager.add("name", "data")

        val exception = assertThrows(AssertionError::class.java) {
            manager.assertHasWork("name", "data")
        }

        assertEquals(
            "Does contains more then one work: name with data: [data]",
            exception.message
        )
    }

    @Test
    fun `Given two same works with different data When asserting for one work Then fail`() {
        manager.add("name", "data1")
        manager.add("name", "data2")

        manager.assertHasWork("name", "data1")
    }

    @Test
    fun `Given work with different data When asserting for work Then fail`() {
        manager.add("name", "different-data")

        val exception = assertThrows(AssertionError::class.java) {
            manager.assertHasWork("name", "data")
        }

        assertEquals(
            "Does not contains work: name with the same data, expected:[data] but was: [[different-data]]",
            exception.message
        )
    }
}