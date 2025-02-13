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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BackupErrorRepository
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository
import me.proton.core.drive.backup.domain.usecase.FindDuplicates
import me.proton.core.drive.backup.domain.usecase.HandleBackupError
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.data.api.request.CheckAvailableHashesRequest
import me.proton.core.drive.link.data.api.response.CheckAvailableHashesResponse
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.checkAvailableHashes
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.request
import me.proton.core.drive.test.api.retryableErrorResponse
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class BackupFindDuplicatesWorkerTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var backupFolderRepository: BackupFolderRepository

    @Inject
    lateinit var backupFileRepository: BackupFileRepository

    @Inject
    lateinit var backupErrorRepository: BackupErrorRepository

    @Inject
    lateinit var findDuplicates: FindDuplicates

    @Inject
    lateinit var configurationProvider: ConfigurationProvider

    @Inject
    lateinit var handleBackupError: HandleBackupError
    private lateinit var backupFolder: BackupFolder
    private lateinit var backupFiles: List<BackupFile>

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }

        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )
        backupFolderRepository.insertFolder(backupFolder)
        backupFiles = listOf(
            backupFile(index = 3, backupFileState = BackupFileState.IDLE),
            backupFile(index = 2, backupFileState = BackupFileState.IDLE),
            backupFile(index = 1, backupFileState = BackupFileState.IDLE),
        )
        backupFileRepository.insertFiles(
            backupFiles = backupFiles
        )
    }

    @Test
    fun success() = runTest {
        driveRule.server.checkAvailableHashes {
            jsonResponse {
                CheckAvailableHashesResponse(
                    code = ProtonApiCode.SUCCESS,
                    availableHashes = request<CheckAvailableHashesRequest>().hashes.filterIndexed { index, _ ->
                        index > 0
                    },
                    pendingHashDtos = emptyList()
                )
            }
        }
        val worker = backupFindDuplicatesWorker(backupFolder)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(
            listOf(
                backupFile(index = 3, backupFileState = BackupFileState.POSSIBLE_DUPLICATE),
                backupFile(index = 2, backupFileState = BackupFileState.READY),
                backupFile(index = 1, backupFileState = BackupFileState.READY),
            ),
            backupFileRepository.getAllFiles(folderId, 0, 100),
        )
        assertEquals(
            emptyList<BackupError>(),
            backupErrorRepository.getAll(folderId, 0, 100).first()
        )
    }

    @Test
    fun retry() = runTest {
        driveRule.server.checkAvailableHashes { retryableErrorResponse() }
        val worker = backupFindDuplicatesWorker(backupFolder)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(
            backupFiles,
            backupFileRepository.getAllFiles(folderId, 0, 100),
        )
        assertEquals(
            emptyList<BackupError>(),
            backupErrorRepository.getAll(folderId, 0, 100).first()
        )
    }

    @Test
    fun failure() = runTest {
        driveRule.server.checkAvailableHashes { retryableErrorResponse() }
        val worker = backupFindDuplicatesWorker(backupFolder, 11)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals(
            backupFiles,
            backupFileRepository.getAllFiles(folderId, 0, 100),
        )
        assertEquals(
            listOf(BackupError.Other(true)),
            backupErrorRepository.getAll(folderId, 0, 100).first()
        )
    }

    private fun backupFindDuplicatesWorker(
        backupFolder: BackupFolder,
        runAttemptCount: Int = 1,
    ): BackupFindDuplicatesWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<BackupFindDuplicatesWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = BackupFindDuplicatesWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    findDuplicates = findDuplicates,
                    configurationProvider = configurationProvider,
                    handleBackupError = handleBackupError,
                )

            })
            .setInputData(
                BackupFindDuplicatesWorker.workDataOf(
                    backupFolder
                )
            )
            .setRunAttemptCount(runAttemptCount)
            .build()
    }

    private fun backupFile(index: Int, backupFileState: BackupFileState) = backupFile(
        uriString = "uri$index",
        hash = "hash$index",
        backupFileState = backupFileState,
        date = TimestampS(index.toLong()),
    )

    private fun backupFile(
        uriString: String,
        hash: String,
        backupFileState: BackupFileState,
        date: TimestampS = TimestampS(0),
    ) =
        BackupFile(
            bucketId = 0,
            folderId = folderId,
            uriString = uriString,
            mimeType = "",
            name = "",
            hash = hash,
            size = 0.bytes,
            state = backupFileState,
            date = date,
        )
}
