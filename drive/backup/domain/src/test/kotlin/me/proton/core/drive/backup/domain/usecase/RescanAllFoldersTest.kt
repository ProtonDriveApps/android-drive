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

import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FolderId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RescanAllFoldersTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var backupManager: StubbedBackupManager
    private lateinit var addFolder: AddFolder
    private lateinit var rescanAllFolders: RescanAllFolders

    private lateinit var backupFolder: BackupFolder

    @Before
    fun setup() = runTest {
        folderId = database.myFiles { }
        val folderRepository = BackupFolderRepositoryImpl(database.db)

        backupManager = StubbedBackupManager(folderRepository)
        addFolder = AddFolder(folderRepository)
        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
            updateTime = TimestampS(1),
        )
        addFolder(backupFolder).getOrThrow()
        rescanAllFolders = RescanAllFolders(
            resetFoldersUpdateTime = ResetFoldersUpdateTime(folderRepository),
            syncFolders = SyncFolders(
                getAllFolders = GetAllFolders(folderRepository),
                backupManager = backupManager,
            )
        )
    }


    @Test
    fun test() = runTest {
        rescanAllFolders(folderId).getOrThrow()

        assertEquals(listOf(backupFolder.copy(updateTime = null)), backupManager.sync)
    }

}
