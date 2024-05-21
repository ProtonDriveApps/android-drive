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

package me.proton.core.drive.backup.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.TestScanFolderRepository
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.AddFolder
import me.proton.core.drive.backup.domain.usecase.ScanFolder
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
class BackupScanFolderRepositoryWorkerTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var scanFolder: ScanFolder

    @Inject
    lateinit var scanFolderRepository: TestScanFolderRepository

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var addBackupError: AddBackupError

    @Inject
    lateinit var fileRepository: BackupFileRepository

    @Inject
    lateinit var folderRepository: BackupFolderRepository

    private val bucketId = 0

    private lateinit var backupFile1: BackupFile
    private lateinit var backupFile2: BackupFile

    private lateinit var backupFolder: BackupFolder

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )
        addFolder(backupFolder).getOrThrow()

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
        driveRule.server.getPublicAddressKeys()
    }

    @Test
    fun `Given medias when sync a folder then should store those medias`() = runTest {
        scanFolderRepository.backupFiles = listOf(backupFile1, backupFile2)

        val worker = backupScanFolderWorker(backupFolder)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(
            listOf(
                backupFile2.copy(hash = "02e38b29626f14b2ad31e4295a5b7e4be8adff44626cb2c2bdb49a026b7a85c5"),
                backupFile1.copy(hash = "9bf837b813a24c0e9f06a840eee4ea2c5e60e81147d44188cb3544ecce394270"),
            ), fileRepository.getAllFiles(
                folderId = folderId, fromIndex = 0, count = 2
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
        return TestListenableWorkerBuilder<BackupScanFolderWorker>(context).setWorkerFactory(object :
                WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = BackupScanFolderWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    scanFolder = scanFolder,
                    addBackupError = addBackupError,
                )

            }).setInputData(
                BackupScanFolderWorker.workDataOf(
                    backupFolder, UploadFileLink.BACKUP_PRIORITY
                )
            ).build()
    }
}
