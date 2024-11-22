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

package me.proton.core.drive.drivelink.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.block
import me.proton.core.drive.db.test.download
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.drivelink.domain.repository.DriveLinkRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DriveLinkRepositoryImplTest {

    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var repository: DriveLinkRepository

    @Before
    fun setUp() {
        repository = DriveLinkRepositoryImpl(database.db.driveLinkDao)
    }

    @Test
    fun `Given empty folder when count then should count zero`() = runTest {
        val folderId = database.myFiles { }
        val count = repository.getDriveLinksCount(folderId).first()
        assertEquals(0, count)
    }

    @Test
    fun `Given folder with one file when count then should count one`() = runTest {
        val folderId = database.myFiles {
            file("file")
        }
        val count = repository.getDriveLinksCount(folderId).first()
        assertEquals(1, count)
    }

    @Test
    fun `Given folder with a file of two blocks when count then should count one`() = runTest {
        val folderId = database.myFiles {
            file("file") {
                download {
                    block(0)
                    block(1)
                }
            }
        }
        val count = repository.getDriveLinksCount(folderId).first()
        assertEquals(1, count)
    }

    @Test
    fun `Given folder with a 10 large files with 10 blocks when get drive links then should 10`() = runTest {
        val folderId = database.myFiles {
            for(fileIndex in 1..10) {
                file("file_$fileIndex") {
                    download {
                        for(blockIndex in 1..10) {
                            block(blockIndex.toLong())
                        }
                    }
                }
            }
        }
        val driveLinks = repository.getDriveLinks(folderId, 0, 50).first()
        assertEquals(10, driveLinks.size)
    }
}
