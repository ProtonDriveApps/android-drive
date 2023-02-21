/*
 * Copyright (c) 2022-2023 Proton AG.
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

package me.proton.core.drive.base.domain.entity

import junit.framework.TestCase.assertEquals
import org.junit.Test

class BytesTest {

    @Test
    fun `plus bytes`() {
        // region Arrange
        val first = Bytes(2L)
        val second = Bytes(3L)
        // endregion
        // region Act
        val result = first + second
        // endregion
        // region Assert
        assertEquals(5L, result.value)
        // endregion
    }

    @Test
    fun `minus bytes`() {
        // region Arrange
        val first = Bytes(2L)
        val second = Bytes(3L)
        // endregion
        // region Act
        val result = first - second
        // endregion
        // region Assert
        assertEquals(-1L, result.value)
        // endregion
    }

    @Test
    fun `times bytes`() {
        // region Arrange
        val first = Bytes(2L)
        val second = Bytes(3L)
        // endregion
        // region Act
        val result = first * second
        // endregion
        // region Assert
        assertEquals(6L, result.value)
        // endregion
    }



    @Test
    fun `times float`() {
        // region Arrange
        val first = Bytes(2L)
        val second = 0.9f
        // endregion
        // region Act
        val result = first * second
        // endregion
        // region Assert
        assertEquals(1L, result.value)
        // endregion
    }

    @Test
    fun `div bytes`() {
        // region Arrange
        val first = Bytes(10L)
        val second = Bytes(2L)
        // endregion
        // region Act
        val result = first / second
        // endregion
        // region Assert
        assertEquals(5.0, result)
        // endregion
    }

    @Test
    fun `compare to greater bytes`() {
        // region Arrange
        val first = Bytes(1L)
        val second = Bytes(2L)
        // endregion
        // region Act
        val result = first.compareTo(second)
        // endregion
        // region Assert
        assertEquals(-1, result)
        // endregion
    }

    @Test
    fun `compare to equal bytes`() {
        // region Arrange
        val first = Bytes(1L)
        val second = Bytes(1L)
        // endregion
        // region Act
        val result = first.compareTo(second)
        // endregion
        // region Assert
        assertEquals(0, result)
        // endregion
    }

    @Test
    fun `compare to lower bytes`() {
        // region Arrange
        val first = Bytes(2L)
        val second = Bytes(1L)
        // endregion
        // region Act
        val result = first.compareTo(second)
        // endregion
        // region Assert
        assertEquals(1, result)
        // endregion
    }
}