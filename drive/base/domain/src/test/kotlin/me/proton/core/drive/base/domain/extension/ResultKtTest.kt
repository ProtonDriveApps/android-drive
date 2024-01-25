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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ResultKtTest {

    @Test
    fun `given all success when throwOnFailure should not nothing`() {
        listOf(
            Result.success(Unit),
            Result.success(Unit),
            Result.success(Unit),
        ).throwOnFailure {
            error("Should not be called")
        }
    }

    @Test
    fun `given two failures when throwOnFailure should throw an exception`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            listOf(
                Result.success(Unit),
                Result.failure(Throwable()),
                Result.failure(Throwable()),
            ).throwOnFailure { count ->
                error("$count failures")
            }
        }
        assertEquals("2 failures", exception.message)
    }
}
