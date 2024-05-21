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

package me.proton.core.drive.backup.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class FoldersTest {
    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var updateFolder: UpdateFolder

    @Inject
    lateinit var deleteFolder: DeleteFolder

    @Inject
    lateinit var deleteFolders: DeleteFolders

    @Inject
    lateinit var getAllFolders: GetAllFolders

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
    }

    @Test
    fun empty() = runTest {
        assertEquals(emptyList<BackupFolder>(), getAllFolders(folderId).getOrThrow())
    }

    @Test
    fun add() = runTest {
        val backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )

        addFolder(backupFolder).getOrThrow()

        assertEquals(listOf(backupFolder), getAllFolders(folderId).getOrThrow())
    }

    @Test
    fun deleteAll() = runTest {
        val backupFolder1 = BackupFolder(
            bucketId = 1,
            folderId = folderId,
        )
        val backupFolder2 = BackupFolder(
            bucketId = 2,
            folderId = folderId,
        )

        addFolder(backupFolder1).getOrThrow()
        addFolder(backupFolder2).getOrThrow()

        deleteFolders(folderId).getOrThrow()

        assertEquals(emptyList<BackupFolder>(), getAllFolders(folderId).getOrThrow())
    }

    @Test
    fun delete() = runTest {
        val backupFolder1 = BackupFolder(
            bucketId = 1,
            folderId = folderId,
        )
        val backupFolder2 = BackupFolder(
            bucketId = 2,
            folderId = folderId,
        )

        addFolder(backupFolder1).getOrThrow()
        addFolder(backupFolder2).getOrThrow()

        deleteFolder(backupFolder1).getOrThrow()

        assertEquals(listOf(backupFolder2), getAllFolders(folderId).getOrThrow())
    }

    @Test
    fun update() = runTest {
        val updateTime = TimestampS(1000)
        val bucketId = 0
        val backupFolder = BackupFolder(
            bucketId = bucketId,
            folderId = folderId,
        )

        addFolder(backupFolder).getOrThrow()
        updateFolder(backupFolder.copy(updateTime = updateTime)).getOrThrow()

        assertEquals(
            listOf(backupFolder.copy(updateTime = updateTime)),
            getAllFolders(folderId).getOrThrow(),
        )
    }
}
