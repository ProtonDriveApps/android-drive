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

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.pgp.HashKey
import me.proton.core.crypto.common.pgp.hmacSha256
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.ScanFolderRepository
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.crypto.domain.usecase.base.UseHashKey
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScanFolderTest {

    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var scanFolder: ScanFolder
    private lateinit var fileRepository: BackupFileRepositoryImpl
    private lateinit var folderRepository: BackupFolderRepositoryImpl

    private var backupFiles = emptyList<BackupFile>()

    private val scanFolderRepository = ScanFolderRepository { bucketId, timestamp ->
        backupFiles.filter { backupFile ->
            val sameFolder = backupFile.bucketId == bucketId
            val notSynced = if (timestamp == null) {
                true
            } else {
                backupFile.date > timestamp
            }
            sameFolder && notSynced
        }
    }

    private val bucketId = 0

    private val backupFile1 = NullableBackupFile(
        bucketId = bucketId,
        uriString = "uri1",
        name = "file1",
        date = TimestampS(1000),
    )
    private val backupFile2 = NullableBackupFile(
        bucketId = bucketId,
        uriString = "uri2",
        name = "file2",
        date = TimestampS(2000),
    )

    private lateinit var backupFolder: BackupFolder

    @Before
    fun setUp() = runTest {
        val folderId = database.myDrive { }
        backupFolder = BackupFolder(
            bucketId = bucketId,
            folderId = folderId,
        )
        fileRepository = BackupFileRepositoryImpl(database.db)
        folderRepository = BackupFolderRepositoryImpl(database.db)
        folderRepository.insertFolder(backupFolder)
        val useHashKey = mockk<UseHashKey>()
        val hashKey = mockk<HashKey>()
        mockkStatic(HashKey::hmacSha256)
        coEvery { useHashKey<BackupFile>(folderId, any(), any(), any()) } coAnswers {
            val block = arg<suspend (HashKey) -> BackupFile>(3)
            Result.success(block(hashKey))
        }
        coEvery { hashKey.hmacSha256(any()) } answers {
            val input = secondArg<String>()
            "hmacSha256($input)"
        }
        scanFolder = ScanFolder(
            scanFolder = scanFolderRepository,
            setFiles = SetFiles(fileRepository),
            updateFolder = UpdateFolder(folderRepository),
            useHashKey = useHashKey,
        )
    }

    @Test
    fun `Given no medias when sync all folders then should store nothing`() = runTest {

        val result = scanFolder(userId, backupFolder, UploadFileLink.BACKUP_PRIORITY)

        assertEquals(Result.success(backupFiles), result)
        assertEquals(
            backupFiles,
            fileRepository.getAllFiles(userId = userId, fromIndex = 0, count = 1),
        )
    }

    @Test
    fun `Given medias when sync a folder then should store those medias`() =
        runTest {
            backupFiles = listOf(backupFile1, backupFile2)

            val result = scanFolder(userId, backupFolder, UploadFileLink.BACKUP_PRIORITY)

            assertEquals(
                Result.success(
                    listOf(
                        backupFile1.copy(hash = "hmacSha256(file1)"),
                        backupFile2.copy(hash = "hmacSha256(file2)"),
                    )
                ), result
            )
            assertEquals(
                listOf(
                    backupFile2.copy(hash = "hmacSha256(file2)"),
                    backupFile1.copy(hash = "hmacSha256(file1)"),
                ),
                fileRepository.getAllFiles(userId = userId, fromIndex = 0, count = 2)
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
            backupFiles = listOf(
                backupFile1,
                backupFile2
            )
            folderRepository.updateFolderUpdateTime(userId, bucketId, updateTime)

            val result = scanFolder(
                userId,
                backupFolder.copy(updateTime = updateTime),
                UploadFileLink.BACKUP_PRIORITY
            )

            val resultBackupFiles = listOf(
                backupFile2.copy(hash = "hmacSha256(file2)"),
            )
            assertEquals(Result.success(resultBackupFiles), result)
            assertEquals(
                resultBackupFiles,
                fileRepository.getAllFiles(userId = userId, fromIndex = 0, count = 2),
            )
            assertEquals(
                backupFile2.date,
                folderRepository.getAll(userId).first().updateTime!!,
            )
        }
}
