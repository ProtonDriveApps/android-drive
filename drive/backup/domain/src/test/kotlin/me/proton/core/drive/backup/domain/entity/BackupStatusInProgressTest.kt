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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BackupStatusInProgressTest(
    private val inProgress: BackupStatus.InProgress,
    private val progress: Float,
) {
    @Test
    fun test() {
        assertEquals(progress, inProgress.progress)
    }

    companion object {

        private fun progressOf(total: Int, pending: Int) =
            BackupStatus.InProgress(total, pending)

        @get:Parameterized.Parameters(name = "Given in progress of {0} should calculate {1}")
        @get:JvmStatic
        val data = listOf(
            arrayOf(progressOf(100, 0), 1F),
            arrayOf(progressOf(100, 50), 0.5F),
            arrayOf(progressOf(100, 100), 0F),
        )
    }

}
