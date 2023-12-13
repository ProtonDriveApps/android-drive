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

package me.proton.core.drive.backup.domain.entity

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BackupStatusInProgressErrorTest(
    private val total: Int,
    private val pending: Int,
) {
    @Test(expected = IllegalArgumentException::class)
    fun test() {
        BackupStatus.InProgress(total, pending)
    }

    companion object {

        @get:Parameterized.Parameters(name = "Given in progress of {0} should calculate {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(0, 0),
            arrayOf(100, 150),
            arrayOf(100, -50),
            arrayOf(-100, -50),
        )
    }
}
