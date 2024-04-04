/*
 * Copyright (c) 2023-2024 Proton AG.
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

package me.proton.core.drive.stats.domain.usecase

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.stats.data.repository.UploadStatsRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UploadStatsTest {
    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var getUploadStats: GetUploadStats
    private lateinit var updateUploadStats: UpdateUploadStats
    private lateinit var deleteUploadStats: DeleteUploadStats

    private lateinit var folderId: FolderId

    @Before
    fun setUp() = runTest {
        folderId = database.myFiles {}
        val repository = UploadStatsRepositoryImpl(database.db)
        getUploadStats = GetUploadStats(repository)
        updateUploadStats = UpdateUploadStats(repository)
        deleteUploadStats = DeleteUploadStats(repository)
    }

    @Test
    fun empty() = runTest {
        assertThrows(NoSuchElementException::class.java) {
            runBlocking { getUploadStats(folderId).getOrThrow() }
        }
    }

    @Test
    fun identity() = runTest {
        val progress = progress(1.bytes)
        updateUploadStats(progress).getOrThrow()
        assertEquals(progress, getUploadStats(folderId).getOrThrow())
    }

    @Test
    fun delete() = runTest {
        val progress = progress(1.bytes)
        updateUploadStats(progress).getOrThrow()
        deleteUploadStats(folderId).getOrThrow()
        assertThrows(NoSuchElementException::class.java) {
            runBlocking { getUploadStats(folderId).getOrThrow() }
        }
    }

    @Test
    fun aggregate() = runTest {
        updateUploadStats(
            progress(
                size = 1.bytes,
                minimumUploadCreationDateTime = TimestampS(10),
                minimumFileCreationDateTime = TimestampS(1)
            )
        ).getOrThrow()
        updateUploadStats(
            progress(
                size = 5.bytes,
                minimumUploadCreationDateTime = TimestampS(20),
                minimumFileCreationDateTime = TimestampS(2)
            )
        ).getOrThrow()
        updateUploadStats(
            progress(
                size = 10.bytes,
                minimumUploadCreationDateTime = TimestampS(30),
                minimumFileCreationDateTime = TimestampS(3)
            )
        ).getOrThrow()
        assertEquals(
            progress(
                size = 16.bytes,
                minimumUploadCreationDateTime = TimestampS(10),
                minimumFileCreationDateTime = TimestampS(1),
                count = 3,
            ),
            getUploadStats(folderId).getOrThrow(),
        )
    }

    private fun progress(
        size: Bytes,
        minimumUploadCreationDateTime: TimestampS = TimestampS(),
        minimumFileCreationDateTime: TimestampS = TimestampS(),
        count: Long = 1,
    ) =
        me.proton.core.drive.stats.domain.entity.UploadStats(
            folderId = folderId,
            count = count,
            size = size,
            minimumUploadCreationDateTime = minimumUploadCreationDateTime,
            minimumFileCreationDateTime = minimumFileCreationDateTime,
        )
}
