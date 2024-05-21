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

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SyncStaleFoldersTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var syncStaleFolders: SyncStaleFolders

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var backupManager: StubbedBackupManager

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
    }

    @Test
    fun `Given folder without sync time when sync stale then should do nothing`() =
        runTest {
            val backupFolder = BackupFolder(
                bucketId = 0,
                folderId = folderId,
                syncTime = null
            )
            addFolder(backupFolder).getOrThrow()

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
            addFolder(backupFolder).getOrThrow()

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
            addFolder(backupFolder).getOrThrow()

            val folders = syncStaleFolders(folderId, UploadFileLink.BACKUP_PRIORITY).getOrThrow()

            assertEquals(listOf(backupFolder), folders)
            assertEquals(listOf(backupFolder), backupManager.sync)
        }
}
