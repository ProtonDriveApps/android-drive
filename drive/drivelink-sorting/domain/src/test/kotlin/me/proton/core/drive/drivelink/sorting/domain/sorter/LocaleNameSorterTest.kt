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

package me.proton.core.drive.drivelink.sorting.domain.sorter

import me.proton.core.drive.sorting.domain.entity.By
import me.proton.core.drive.sorting.domain.entity.Direction
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class LocaleNameSorterTest {

    private val File1 = file("File1")
    private val file2 = file("file2")
    private val file22 = file("file22")
    private val file3 = file("file3")
    private val file4 = file("file4")
    private val file45 = file("file45")
    private val file5 = file("file5")

    private val files = listOf(
        file2,
        File1,
        file4,
        file22,
        file3,
        file45,
        file5,
    )

    @Test
    @Config(sdk = [24])
    fun `sort files by locale names ascending`() {

        val sorted = Sorter.Factory[By.NAME].sort(files, Direction.ASCENDING)

        assertEquals(
            listOf(
                File1,
                file2,
                file3,
                file4,
                file5,
                file22,
                file45,
            ).map { it.name },
            sorted.map { it.name },
        )
    }

    @Test
    @Config(sdk = [24])
    fun `sort files by locale names descending`() {

        val sorted = Sorter.Factory[By.NAME].sort(files, Direction.DESCENDING)

        assertEquals(
            listOf(
                file45,
                file22,
                file5,
                file4,
                file3,
                file2,
                File1,
            ).map { it.name },
            sorted.map { it.name },
        )
    }

    @Test
    @Config(sdk = [23])
    fun `sort files by names ascending`() {

        val sorted = Sorter.Factory[By.NAME].sort(files, Direction.ASCENDING)

        assertEquals(
            listOf(
                File1,
                file2,
                file22,
                file3,
                file4,
                file45,
                file5,
            ).map { it.name },
            sorted.map { it.name },
        )
    }

    @Test
    @Config(sdk = [23])
    fun `sort files by names descending`() {

        val sorted = Sorter.Factory[By.NAME].sort(files, Direction.DESCENDING)

        assertEquals(
            listOf(
                file5,
                file45,
                file4,
                file3,
                file22,
                file2,
                File1,
            ).map { it.name },
            sorted.map { it.name },
        )
    }
}