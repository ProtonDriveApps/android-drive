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
import me.proton.core.drive.backup.domain.entity.BucketUpdate
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
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

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SyncFoldersTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var syncFolders: SyncFolders

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var backupManager: StubbedBackupManager

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
    }

    @Test
    fun `Given folders when sync then should sync each folder`() = runTest {
        val backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )
        addFolder(backupFolder).getOrThrow()

        val folders = syncFolders(folderId, UploadFileLink.BACKUP_PRIORITY).getOrThrow()

        assertEquals(listOf(backupFolder), folders)
        assertEquals(listOf(backupFolder), backupManager.sync)
    }

    @Test
    fun `Given three folders when sync two buckets older and newer then should sync two folders with min time`() =
        runTest {
            val backupFolder1 = BackupFolder(
                bucketId = 1,
                folderId = folderId,
                updateTime = TimestampS(10),
            )
            val backupFolder2 = BackupFolder(
                bucketId = 2,
                folderId = folderId,
                updateTime = TimestampS(20),
            )
            val backupFolder3 = BackupFolder(
                bucketId = 3,
                folderId = folderId,
                updateTime = TimestampS(30),
            )
            addFolder(backupFolder1).getOrThrow()
            addFolder(backupFolder2).getOrThrow()
            addFolder(backupFolder3).getOrThrow()

            val result = syncFolders(
                userId, listOf(
                    BucketUpdate(bucketId = 1, oldestAddedTime = TimestampS(5)),
                    BucketUpdate(bucketId = 3, oldestAddedTime = TimestampS(35)),
                ), UploadFileLink.BACKUP_PRIORITY
            ).getOrThrow()

            assertEquals(
                listOf(
                    backupFolder1.copy(updateTime = TimestampS(5)), backupFolder3
                ),
                result,
            )
            assertEquals(
                listOf(
                    backupFolder1.copy(updateTime = TimestampS(5)), backupFolder3
                ),
                backupManager.sync,
            )
        }
}
