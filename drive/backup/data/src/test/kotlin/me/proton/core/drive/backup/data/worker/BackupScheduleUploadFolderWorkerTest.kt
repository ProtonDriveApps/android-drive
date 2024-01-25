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
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration

@RunWith(RobolectricTestRunner::class)
class BackupScheduleUploadFolderWorkerTest {
    @get:Rule
    val database = DriveDatabaseRule()

    private val bucketId = 0


    private lateinit var backupFolder: BackupFolder

    @Before
    fun setUp() = runTest {
        val folderId = database.myDrive { }
        backupFolder = BackupFolder(
            bucketId = bucketId,
            folderId = folderId,
        )
    }

    @Test
    fun create() = runTest {
        val worker = backupScheduleUploadFolderWorker(backupFolder)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    private fun backupScheduleUploadFolderWorker(
        backupFolder: BackupFolder,
        delay: Duration = Duration.ZERO,
    ): BackupScheduleUploadFolderWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<BackupScheduleUploadFolderWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ) = BackupScheduleUploadFolderWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    workManager = mockk(relaxed = true),
                )

            })
            .setInputData(
                BackupScheduleUploadFolderWorker.workDataOf(
                    backupFolder = backupFolder,
                    delay = delay,
                )
            )
            .build()
    }

}
