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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FoldersTest {
    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var addFolder: AddFolder
    private lateinit var updateFolder: UpdateFolder
    private lateinit var deleteFolders: DeleteFolders
    private lateinit var getFolders: GetFolders

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive { }
        val repository = BackupFolderRepositoryImpl(database.db)
        addFolder = AddFolder(repository)
        updateFolder = UpdateFolder(repository)
        deleteFolders = DeleteFolders(repository)
        getFolders = GetFolders(repository)
    }

    @Test
    fun empty() = runTest {
        assertEquals(emptyList<BackupFolder>(), getFolders(userId).getOrThrow())
    }

    @Test
    fun add() = runTest {
        val backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )

        addFolder(backupFolder)

        assertEquals(listOf(backupFolder), getFolders(userId).getOrThrow())
    }

    @Test
    fun delete() = runTest {
        val backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )

        addFolder(backupFolder)

        deleteFolders(userId)

        assertEquals(emptyList<BackupFolder>(), getFolders(userId).getOrThrow())
    }

    @Test
    fun update() = runTest {
        val updateTime = TimestampS(1000)
        val bucketId = 0
        val backupFolder = BackupFolder(
            bucketId = bucketId,
            folderId = folderId,
        )

        addFolder(backupFolder)
        updateFolder(userId, bucketId, updateTime)

        assertEquals(
            listOf(backupFolder.copy(updateTime = updateTime)),
            getFolders(userId).getOrThrow(),
        )
    }
}