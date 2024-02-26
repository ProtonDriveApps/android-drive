/*
 * Copyright (c) 2024 Proton AG.
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
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@RunWith(RobolectricTestRunner::class)
class SyncStaleFoldersTest {

    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var syncStaleFolders: SyncStaleFolders
    private lateinit var repository: BackupFolderRepositoryImpl

    private lateinit var folderId: FolderId

    private lateinit var backupManager: StubbedBackupManager

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive { }

        repository = BackupFolderRepositoryImpl(database.db)
        backupManager = StubbedBackupManager(repository)
        syncStaleFolders = SyncStaleFolders(
            getAllFolders = GetAllFolders(repository),
            backupManager = backupManager,
            configurationProvider = object : ConfigurationProvider {
                override val host: String = ""
                override val baseUrl: String = ""
                override val appVersionHeader: String = ""
                override val backupSyncWindow: Duration = 1.days
            },
        )
    }

    @Test
    fun `Given folder without sync time when sync stale then should do nothing`() =
        runTest {
            val backupFolder = BackupFolder(
                bucketId = 0,
                folderId = folderId,
                syncTime = null
            )
            repository.insertFolder(backupFolder)

            val folders = syncStaleFolders(folderId, UploadFileLink.BACKUP_PRIORITY).getOrThrow()

            assertEquals(emptyList<BackupFolder>(), folders)
            assertEquals(emptyList<BackupFolder>(), backupManager.sync)
        }

    @Test
    fun `Given folders with recent sync time when sync then should do nothing`() =
        runTest {
            val backupFolder = BackupFolder(
                bucketId = 0,
                folderId = folderId,
                syncTime = TimestampS()
            )
            repository.insertFolder(backupFolder)

            val folders = syncStaleFolders(folderId, UploadFileLink.BACKUP_PRIORITY).getOrThrow()

            assertEquals(emptyList<BackupFolder>(), folders)
            assertEquals(emptyList<BackupFolder>(), backupManager.sync)
        }

    @Test
    fun `Given folders old sync time when sync then should sync folder`() =
        runTest {
            val now = TimestampS()
            val backupFolder = BackupFolder(
                bucketId = 0,
                folderId = folderId,
                syncTime = TimestampS(now.value - 1.1.days.inWholeSeconds)
            )
            repository.insertFolder(backupFolder)

            val folders = syncStaleFolders(folderId, UploadFileLink.BACKUP_PRIORITY).getOrThrow()

            assertEquals(listOf(backupFolder), folders)
            assertEquals(listOf(backupFolder), backupManager.sync)
        }
}
