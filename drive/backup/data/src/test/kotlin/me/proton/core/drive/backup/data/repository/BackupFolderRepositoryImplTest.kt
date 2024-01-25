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

package me.proton.core.drive.backup.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupFolderRepositoryImplTest {

    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var folderId: FolderId
    private lateinit var repository: BackupFolderRepositoryImpl

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive { }
        repository = BackupFolderRepositoryImpl(database.db)
    }

    @Test
    fun `Given no data when get all should returns nothing`() = runTest {
        val folders = repository.getAll(userId)

        assertEquals(emptyList<BackupFolder>(), folders)
    }

    @Test
    fun `Given a folder when get all should returns this folder`() = runTest {
        val folder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )

        repository.insertFolder(folder)

        val folders = repository.getAll(userId)
        assertEquals(listOf(folder), folders)
    }

    @Test
    fun `Given a folder when update time and get all should returns this folder with updated time`() =
        runTest {
            val folder = BackupFolder(
                bucketId = 0,
                folderId = folderId,
            )
            repository.insertFolder(folder)
            val updateTime = TimestampS(1)

            repository.updateFolderUpdateTime(userId, 0, updateTime)

            val folders = repository.getAll(userId)
            assertEquals(listOf(folder.copy(updateTime = updateTime)), folders)
        }

    @Test
    fun `Given a folder when delete it and get all should returns nothing`() =
        runTest {
            val folder = BackupFolder(
                bucketId = 0,
                folderId = folderId,
            )
            repository.insertFolder(folder)

            repository.deleteFolders(folderId)

            val folders = repository.getAll(userId)
            assertEquals(emptyList<BackupFolder>(), folders)
        }


    @Test
    fun `Given no data when check has folders should returns false`() = runTest {
        val hasFolders = repository.hasFolders(userId)

        assertFalse(hasFolders.first())
    }

    @Test
    fun `Given a folder when check has folders for user should returns true`() = runTest {
        val hasFolders = repository.hasFolders(userId)

        val folder = BackupFolder(0, folderId)
        repository.insertFolder(folder)

        assertTrue(hasFolders.first())
    }

    @Test
    fun `Given no data when check has folders for folder should returns false`() = runTest {
        val hasFolders = repository.hasFolders(folderId)

        assertFalse(hasFolders.first())
    }

    @Test
    fun `Given a folder when check has folders for folder should returns true`() = runTest {
        val hasFolders = repository.hasFolders(folderId)

        val folder = BackupFolder(0, folderId)
        repository.insertFolder(folder)

        assertTrue(hasFolders.first())
    }

    @Test
    fun `Given a child folder when check has folders for folder should returns false`() = runTest {
        folderId = database.myDrive {
            folder("child")
        }
        val hasFolders = repository.hasFolders(folderId)

        val folder = BackupFolder(0, FolderId(folderId.shareId, "child"))
        repository.insertFolder(folder)

        assertFalse(hasFolders.first())
    }
}
