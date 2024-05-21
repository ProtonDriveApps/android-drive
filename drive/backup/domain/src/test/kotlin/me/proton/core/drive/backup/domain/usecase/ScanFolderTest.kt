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
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.TestScanFolderRepository
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.getPublicAddressKeys
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ScanFolderTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder
    private val bucketId = 0
    private lateinit var backupFile1: BackupFile
    private lateinit var backupFile2: BackupFile

    @Inject
    lateinit var scanFolder: ScanFolder

    @Inject
    lateinit var fileRepository: BackupFileRepositoryImpl

    @Inject
    lateinit var folderRepository: BackupFolderRepositoryImpl

    @Inject
    lateinit var scanFolderRepository: TestScanFolderRepository

    @Inject
    lateinit var addFolder: AddFolder


    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
        backupFolder = BackupFolder(
            bucketId = bucketId,
            folderId = folderId,
        )
        backupFile1 = NullableBackupFile(
            bucketId = bucketId,
            folderId = folderId,
            uriString = "uri1",
            name = "file1",
            date = TimestampS(1000),
        )
        backupFile2 = NullableBackupFile(
            bucketId = bucketId,
            folderId = folderId,
            uriString = "uri2",
            name = "file2",
            date = TimestampS(2000),
        )
        addFolder(backupFolder).getOrThrow()
        driveRule.server.getPublicAddressKeys()
    }

    @Test
    fun `Given no medias when sync all folders then should store nothing`() = runTest {

        val listResult = scanFolder(backupFolder, UploadFileLink.BACKUP_PRIORITY).getOrThrow()

        assertEquals(scanFolderRepository.backupFiles, listResult)
        assertEquals(
            scanFolderRepository.backupFiles,
            fileRepository.getAllFiles(folderId = folderId, fromIndex = 0, count = 1),
        )
    }

    @Test
    fun `Given medias when sync a folder then should store those medias`() =
        runTest {
            scanFolderRepository.backupFiles = listOf(backupFile1, backupFile2)

            val backupFiles = scanFolder(backupFolder, UploadFileLink.BACKUP_PRIORITY).getOrThrow()

            assertEquals(
                listOf(
                    backupFile1.copy(hash = "9bf837b813a24c0e9f06a840eee4ea2c5e60e81147d44188cb3544ecce394270"),
                    backupFile2.copy(hash = "02e38b29626f14b2ad31e4295a5b7e4be8adff44626cb2c2bdb49a026b7a85c5"),
                ), backupFiles
            )
            assertEquals(
                listOf(
                    backupFile2.copy(hash = "02e38b29626f14b2ad31e4295a5b7e4be8adff44626cb2c2bdb49a026b7a85c5"),
                    backupFile1.copy(hash = "9bf837b813a24c0e9f06a840eee4ea2c5e60e81147d44188cb3544ecce394270"),
                ),
                fileRepository.getAllFiles(folderId = folderId, fromIndex = 0, count = 2)
            )
            assertEquals(
                backupFile2.date,
                folderRepository.getAll(userId).first().updateTime!!,
            )
        }

    @Test
    fun `Given medias already synced when sync a folder then should only store new medias`() =
        runTest {
            val updateTime = backupFile1.date
            scanFolderRepository.backupFiles = listOf(
                backupFile1,
                backupFile2
            )
            folderRepository.updateFolder(backupFolder.copy(updateTime = updateTime))

            val backupFiles = scanFolder(
                backupFolder.copy(updateTime = updateTime),
                UploadFileLink.BACKUP_PRIORITY
            ).getOrThrow()

            val resultBackupFiles = listOf(
                backupFile2.copy(hash = "02e38b29626f14b2ad31e4295a5b7e4be8adff44626cb2c2bdb49a026b7a85c5"),
            )
            assertEquals(resultBackupFiles, backupFiles)
            assertEquals(
                resultBackupFiles,
                fileRepository.getAllFiles(folderId = folderId, fromIndex = 0, count = 2),
            )
            assertEquals(
                backupFile2.date,
                folderRepository.getAll(userId).first().updateTime!!,
            )
        }
}
