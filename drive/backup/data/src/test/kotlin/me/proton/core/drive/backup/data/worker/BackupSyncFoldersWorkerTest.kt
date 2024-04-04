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
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.manager.StubbedBackupManager
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.GetAllFolders
import me.proton.core.drive.backup.domain.usecase.SyncFolders
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupSyncFoldersWorkerTest {

    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var repository: BackupFolderRepositoryImpl
    private lateinit var folderId: FolderId

    private val stubbedBackupManager = StubbedBackupManager()

    @Before
    fun setUp() = runTest {
        folderId = database.myFiles { }

        repository = BackupFolderRepositoryImpl(database.db)
    }

    @Test
    fun `Given folders when sync then should sync each folder`() =
        runTest {
            val backupFolder = BackupFolder(
                bucketId = 0,
                folderId = folderId,
            )
            repository.insertFolder(backupFolder)

            val worker = backupScanFoldersWorker(folderId)
            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(listOf(backupFolder), stubbedBackupManager.sync)
        }

    private fun backupScanFoldersWorker(folderId: FolderId): BackupSyncFoldersWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<BackupSyncFoldersWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {

                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = BackupSyncFoldersWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    syncFolders = SyncFolders(
                        GetAllFolders(repository),
                        stubbedBackupManager,
                    ),
                    addBackupError = AddBackupError(BackupErrorRepositoryImpl(database.db)),
                )

            })
            .setInputData(BackupSyncFoldersWorker.workDataOf(folderId, UploadFileLink.BACKUP_PRIORITY))
            .build()
    }
}
