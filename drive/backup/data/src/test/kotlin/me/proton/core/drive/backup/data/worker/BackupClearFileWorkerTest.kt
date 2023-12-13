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
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.MarkAsCompleted
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupClearFileWorkerTest {


    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var folderId: FolderId

    private lateinit var repository: BackupFileRepositoryImpl

    private val bucketId = 0

    private val backupFile = NullableBackupFile(bucketId, "uri")

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive { }
        val backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        backupFolderRepository.insertFolder(BackupFolder(bucketId, folderId))
        repository = BackupFileRepositoryImpl(database.db)
    }

    @Test
    fun `Given no file when clear should do nothing`() = runTest {
        val worker = backupClearFileWorker(userId, backupFile.uriString)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(
            emptyList<BackupFile>(),
            repository.getAllFiles(userId, 0, 1),
        )
    }

    @Test
    fun `Given one file when clear should mark it as completed`() = runTest {
        repository.insertFiles(userId, listOf(backupFile))

        val worker = backupClearFileWorker(userId, backupFile.uriString)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(
            listOf(backupFile.copy(state = BackupFileState.COMPLETED)),
            repository.getAllFiles(userId, 0, 1),
        )
    }

    private fun backupClearFileWorker(
        userId: UserId,
        uriString: String,
    ): BackupClearFileWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<BackupClearFileWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): BackupClearFileWorker {
                    return BackupClearFileWorker(
                        context = appContext,
                        workerParams = workerParameters,
                        markAsCompleted = MarkAsCompleted(repository),
                        addBackupError = AddBackupError(BackupErrorRepositoryImpl(database.db)),
                    )
                }
            })
            .setInputData(
                BackupClearFileWorker.workDataOf(userId, uriString)
            )
            .build()
    }
}
