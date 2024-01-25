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

package me.proton.core.drive.backup.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.crypto.common.pgp.HashKey
import me.proton.core.crypto.common.pgp.hmacSha256
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.ScanFolderRepository
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.ScanFolder
import me.proton.core.drive.backup.domain.usecase.SetFiles
import me.proton.core.drive.backup.domain.usecase.UpdateFolder
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.crypto.domain.usecase.base.UseHashKey
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupScanFolderRepositoryWorkerTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var scanFolder: ScanFolder
    private lateinit var fileRepository: BackupFileRepositoryImpl
    private lateinit var folderRepository: BackupFolderRepositoryImpl

    private var backupFiles = emptyList<BackupFile>()

    private val scanFolderRepository = ScanFolderRepository { backupFolder ->
        backupFiles.filter { backupFile ->
            val sameFolder = backupFile.bucketId == backupFolder.bucketId
            val timestamp = backupFolder.updateTime
            val notSynced = if (timestamp == null) {
                true
            } else {
                backupFile.date > timestamp
            }
            sameFolder && notSynced
        }
    }

    private val bucketId = 0

    private lateinit var backupFile1: BackupFile
    private lateinit var backupFile2: BackupFile

    private lateinit var backupFolder: BackupFolder

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive { }
        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )
        fileRepository = BackupFileRepositoryImpl(database.db)
        folderRepository = BackupFolderRepositoryImpl(database.db)
        folderRepository.insertFolder(backupFolder)



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
    fun `Given medias when sync a folder then should store those medias`() =
        runTest {
            backupFiles = listOf(backupFile1, backupFile2)

            val worker = backupScanFolderWorker(backupFolder)
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(
                listOf(
                    backupFile2.copy(hash = "hmacSha256(file2)"),
                    backupFile1.copy(hash = "hmacSha256(file1)"),
                ),
                fileRepository.getAllFiles(
                    folderId = folderId,
                    fromIndex = 0,
                    count = 2
                )
            )
            assertEquals(
                backupFile2.date,
                folderRepository.getAll(userId).first().updateTime!!,
            )
        }


    private fun backupScanFolderWorker(
        backupFolder: BackupFolder,
    ): BackupScanFolderWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<BackupScanFolderWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = BackupScanFolderWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    scanFolder = scanFolder,
                    addBackupError = AddBackupError(BackupErrorRepositoryImpl(database.db)),
                )

            })
            .setInputData(
                BackupScanFolderWorker.workDataOf(
                    backupFolder, UploadFileLink.BACKUP_PRIORITY
                )
            )
            .build()
    }
}
