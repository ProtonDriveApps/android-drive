/*
 * Copyright (c) 2023-2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.domain.usecase

import kotlinx.coroutines.test.runTest
import me.proton.android.drive.photos.domain.manager.StubbedBackupManager
import me.proton.android.drive.photos.domain.repository.StubbedMediaStoreVersionRepository
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddFolder
import me.proton.core.drive.backup.domain.usecase.GetAllFolders
import me.proton.core.drive.backup.domain.usecase.RescanAllFolders
import me.proton.core.drive.backup.domain.usecase.ResetFoldersUpdateTime
import me.proton.core.drive.backup.domain.usecase.SyncFolders
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RescanOnMediaStoreUpdateTest {

    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var backupManager: StubbedBackupManager
    private lateinit var addFolder: AddFolder
    private lateinit var rescanAllFolders: RescanAllFolders
    private lateinit var rescanOnMediaStoreUpdate: RescanOnMediaStoreUpdate
    private val mediaStoreVersionRepository = StubbedMediaStoreVersionRepository()

    private lateinit var backupFolder: BackupFolder

    @Before
    fun setup() = runTest {
        val folderId = database.myFiles { }
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
        rescanOnMediaStoreUpdate = RescanOnMediaStoreUpdate(
            mediaStoreVersionRepository = mediaStoreVersionRepository,
            getAllFolders = GetAllFolders(folderRepository),
            rescanAllFolders = rescanAllFolders,
        )
    }

    @Test
    fun `Given null versions when obverse media update should do nothing`() = runTest {
        mediaStoreVersionRepository.apply {
            lastVersion = null
            currentVersion = null
        }

        rescanOnMediaStoreUpdate(userId).getOrThrow()

        assertEquals(emptyList<BackupFolder>(), backupManager.sync)
    }

    @Test
    fun `Given current version when obverse media update should rescan`() = runTest {
        mediaStoreVersionRepository.apply {
            lastVersion = null
            currentVersion = "1"
        }

        rescanOnMediaStoreUpdate(userId).getOrThrow()

        assertEquals(emptyList<BackupFolder>(), backupManager.sync)
    }

    @Test
    fun `Given same versions when obverse media update should do nothing`() = runTest {
        mediaStoreVersionRepository.apply {
            lastVersion = "1"
            currentVersion = "1"
        }

        rescanOnMediaStoreUpdate(userId).getOrThrow()

        assertEquals(emptyList<BackupFolder>(), backupManager.sync)
    }

    @Test
    fun `Given different versions when obverse media update should rescan`() = runTest {
        mediaStoreVersionRepository.apply {
            lastVersion = "1"
            currentVersion = "2"
        }

        rescanOnMediaStoreUpdate(userId).getOrThrow()

        assertEquals(listOf(backupFolder.copy(updateTime = null)), backupManager.sync)
    }
}
