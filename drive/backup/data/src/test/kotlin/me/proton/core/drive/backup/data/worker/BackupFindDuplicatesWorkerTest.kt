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
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.backup.data.repository.BackupDuplicateRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.data.repository.FolderFindDuplicatesRepository
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.FindDuplicatesRepository
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.FindDuplicates
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.ClientUidRepository
import me.proton.core.drive.base.domain.usecase.CreateClientUid
import me.proton.core.drive.base.domain.usecase.CreateUuid
import me.proton.core.drive.base.domain.usecase.GetClientUid
import me.proton.core.drive.base.domain.usecase.GetOrCreateClientUid
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.data.api.LinkApiDataSource
import me.proton.core.drive.link.data.api.response.CheckAvailableHashesResponse
import me.proton.core.drive.link.data.repository.LinkRepositoryImpl
import me.proton.core.drive.link.domain.entity.CheckAvailableHashesInfo
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class BackupFindDuplicatesWorkerTest {

    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var backupFolderRepository: BackupFolderRepositoryImpl
    private lateinit var backupFileRepository: BackupFileRepositoryImpl
    private lateinit var backupErrorRepository: BackupErrorRepositoryImpl
    private lateinit var findDuplicatesRepository: FindDuplicatesRepository
    private val linkApiDataSource: LinkApiDataSource = mockk()
    private lateinit var backupFolder: BackupFolder

    private val backupFiles = listOf(
        backupFile(index = 1, backupFileState = BackupFileState.IDLE),
        backupFile(index = 2, backupFileState = BackupFileState.IDLE),
        backupFile(index = 3, backupFileState = BackupFileState.IDLE),
    )

    @Before
    fun setUp() = runTest {
        backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        backupFileRepository = BackupFileRepositoryImpl(database.db)
        backupErrorRepository = BackupErrorRepositoryImpl(database.db)

        val folderId = database.myDrive { }

        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )
        backupFolderRepository.insertFolder(backupFolder)
        backupFileRepository.insertFiles(
            userId = userId,
            backupFiles = backupFiles
        )
    }

    @Test
    fun success() = runTest {
        coEvery { linkApiDataSource.checkAvailableHashes(backupFolder.folderId, any()) } answers {
            val info: CheckAvailableHashesInfo = secondArg()
            CheckAvailableHashesResponse(
                code = 1000,
                availableHashes = info.hashes.filterIndexed { index, _ -> index > 0 },
                pendingHashDtos = emptyList()
            )
        }
        val worker = backupFindDuplicatesWorker(userId, backupFolder)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(
            listOf(
                backupFile(index = 1, backupFileState = BackupFileState.POSSIBLE_DUPLICATE),
                backupFile(index = 2, backupFileState = BackupFileState.READY),
                backupFile(index = 3, backupFileState = BackupFileState.POSSIBLE_DUPLICATE),
            ),
            backupFileRepository.getAllFiles(userId, 0, 100),
        )
        assertEquals(
            emptyList<BackupError>(),
            backupErrorRepository.getAll(userId, 0, 100).first()
        )
    }

    @Test
    fun retry() = runTest {
        coEvery { linkApiDataSource.checkAvailableHashes(backupFolder.folderId, any()) } throws
                ApiException(ApiResult.Error.Connection())
        val worker = backupFindDuplicatesWorker(userId, backupFolder)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        assertEquals(
            backupFiles,
            backupFileRepository.getAllFiles(userId, 0, 100),
        )
        assertEquals(
            emptyList<BackupError>(),
            backupErrorRepository.getAll(userId, 0, 100).first()
        )
    }

    @Test
    fun failure() = runTest {
        coEvery { linkApiDataSource.checkAvailableHashes(backupFolder.folderId, any()) } throws
                ApiException(ApiResult.Error.Connection())
        val worker = backupFindDuplicatesWorker(userId, backupFolder, 11)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.failure(), result)
        assertEquals(
            backupFiles,
            backupFileRepository.getAllFiles(userId, 0, 100),
        )
        assertEquals(
            listOf(BackupError.Other(true)),
            backupErrorRepository.getAll(userId, 0, 100).first()
        )
    }

    private fun backupFindDuplicatesWorker(
        userId: UserId,
        backupFolder: BackupFolder,
        runAttemptCount: Int = 1,
    ): BackupFindDuplicatesWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configurationProvider = object : ConfigurationProvider {
            override val host: String = ""
            override val baseUrl: String = ""
            override val appVersionHeader: String = ""
            override val apiPageSize: Int = 2
        }
        val linkRepository = LinkRepositoryImpl(linkApiDataSource, database.db)
        findDuplicatesRepository = FolderFindDuplicatesRepository(linkRepository)
        val backupDuplicateRepository = BackupDuplicateRepositoryImpl(database.db)
        val clientUidRepository = object : ClientUidRepository {
            private var clientUid: ClientUid? = null

            override suspend fun get(): ClientUid? = clientUid

            override suspend fun insert(clientUid: ClientUid) {
                this.clientUid = clientUid
            }

        }
        val uuid = UUID(0, 0)
        return TestListenableWorkerBuilder<BackupFindDuplicatesWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = BackupFindDuplicatesWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    findDuplicates = FindDuplicates(
                        configurationProvider = configurationProvider,
                        findDuplicatesRepository = findDuplicatesRepository,
                        backupFileRepository = backupFileRepository,
                        backupDuplicateRepository = backupDuplicateRepository,
                        getOrCreateClientUid = GetOrCreateClientUid(
                            getClientUid = GetClientUid(clientUidRepository),
                            createClientUid = CreateClientUid(
                                repository = clientUidRepository,
                                createUuid = CreateUuid { uuid }
                            )
                        )
                    ),
                    configurationProvider = configurationProvider,
                    addBackupError = AddBackupError(backupErrorRepository),
                )

            })
            .setInputData(
                BackupFindDuplicatesWorker.workDataOf(
                    userId, backupFolder
                )
            )
            .setRunAttemptCount(runAttemptCount)
            .build()
    }
}

private fun backupFile(index: Int, backupFileState: BackupFileState) = backupFile(
    uriString = "uri$index",
    hash = "hash$index",
    backupFileState = backupFileState,
)

private fun backupFile(uriString: String, hash: String, backupFileState: BackupFileState) =
    BackupFile(
        bucketId = 0,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = hash,
        size = 0.bytes,
        state = backupFileState,
        date = TimestampS(0),
    )
