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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.BackupDuplicateRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.CleanRevisions
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.shareId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.folder.domain.repository.FolderRepository
import me.proton.core.drive.folder.domain.usecase.DeleteFolderChildren
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupCleanRevisionsWorkerTest {
    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var folderId: FolderId

    private lateinit var backupDuplicateRepository: BackupDuplicateRepositoryImpl
    private lateinit var backupErrorRepository: BackupErrorRepositoryImpl
    val folderRepository: FolderRepository = mockk()

    private lateinit var backupDuplicates: List<BackupDuplicate>

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive { }
        backupDuplicateRepository = BackupDuplicateRepositoryImpl(database.db)
        backupErrorRepository = BackupErrorRepositoryImpl(database.db)
        backupDuplicates = listOf(
            BackupDuplicate(
                id = 1,
                parentId = folderId,
                hash = "hash",
                contentHash = null,
                linkId = FileId(ShareId(userId, shareId), "link-id"),
                linkState = Link.State.DRAFT,
                revisionId = "revision-Id",
                clientUid = null,
            )
        )
        backupDuplicateRepository.insertDuplicates(backupDuplicates)
    }

    @Test
    fun success() = runTest {
        coEvery { folderRepository.deleteFolderChildren(any(), any()) } returns Result.success(Unit)

        val worker = backupCleanRevisionsWorker(folderId, 0)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(
            emptyList<BackupDuplicate>(),
            backupDuplicateRepository.getAll(folderId, 0, 100)
        )
        assertEquals(
            emptyList<BackupError>(),
            backupErrorRepository.getAll(folderId, 0, 100).first()
        )
    }

    @Test
    fun retry() = runTest {
        coEvery { folderRepository.deleteFolderChildren(any(), any()) } returns
                Result.failure(ApiException(ApiResult.Error.Connection()))

        val worker = backupCleanRevisionsWorker(folderId, 0)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(
            backupDuplicates,
            backupDuplicateRepository.getAll(folderId, 0, 100)
        )
        assertEquals(
            emptyList<BackupError>(),
            backupErrorRepository.getAll(folderId, 0, 100).first()
        )
    }

    @Test
    fun failure() = runTest {
        coEvery { folderRepository.deleteFolderChildren(any(), any()) } returns
                Result.failure(ApiException(ApiResult.Error.Connection()))

        val worker = backupCleanRevisionsWorker(folderId, 11)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals(
            backupDuplicates,
            backupDuplicateRepository.getAll(folderId, 0, 100)
        )
        assertEquals(
            listOf(BackupError.Other(true)),
            backupErrorRepository.getAll(folderId, 0, 100).first()
        )
    }

    private fun backupCleanRevisionsWorker(
        folderId: FolderId,
        runAttemptCount: Int = 1,
    ): BackupCleanRevisionsWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configurationProvider = object : ConfigurationProvider {
            override val host: String = ""
            override val baseUrl: String = ""
            override val appVersionHeader: String = ""
        }
        return TestListenableWorkerBuilder<BackupCleanRevisionsWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = BackupCleanRevisionsWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    cleanRevisions = CleanRevisions(
                        repository = backupDuplicateRepository,
                        configurationProvider = configurationProvider,
                        deleteFolderChildren = DeleteFolderChildren(folderRepository),
                    ),
                    configurationProvider = configurationProvider,
                    addBackupError = AddBackupError(backupErrorRepository)
                )
            })
            .setInputData(
                BackupCleanRevisionsWorker.workDataOf(folderId)
            )
            .setRunAttemptCount(runAttemptCount)
            .build()
    }

}
