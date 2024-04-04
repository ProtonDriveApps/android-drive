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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.BackupDuplicateRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.AddFolder
import me.proton.core.drive.backup.domain.usecase.CheckDuplicates
import me.proton.core.drive.backup.domain.usecase.DeleteFile
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.crypto.domain.usecase.file.GetContentHash
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.upload.domain.resolver.UriResolver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class BackupCheckDuplicatesWorkerTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder

    private val getNodeKey = mockk<GetNodeKey>()
    private val getContentHash = mockk<GetContentHash>()

    private lateinit var checkDuplicates: CheckDuplicates

    private lateinit var backupFileRepository: BackupFileRepositoryImpl
    private lateinit var backupDuplicateRepository: BackupDuplicateRepositoryImpl
    private lateinit var backupErrorRepository: BackupErrorRepositoryImpl

    @Before
    fun setUp() = runTest {

        folderId = database.myFiles { }

        val nodeKey: Key.Node = mockk()
        coEvery { getNodeKey(folderId) } returns Result.success(nodeKey)
        coEvery { getContentHash(folderId, nodeKey, any()) } answers {
            val input: String = arg(2)
            Result.success("getContentHash($input)")
        }

        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId
        )
        val backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        val addFolder = AddFolder(backupFolderRepository)
        addFolder(backupFolder).getOrThrow()
        backupFileRepository = BackupFileRepositoryImpl(database.db)
        backupDuplicateRepository = BackupDuplicateRepositoryImpl(database.db)
        backupErrorRepository = BackupErrorRepositoryImpl(database.db)

        checkDuplicates = CheckDuplicates(
            backupFileRepository = backupFileRepository,
            deleteFile = DeleteFile(backupFileRepository),
            backupDuplicateRepository = BackupDuplicateRepositoryImpl(database.db),
            uriResolver = object : UriResolver {
                override suspend fun <T> useInputStream(
                    uriString: String,
                    block: suspend (InputStream) -> T,
                ): T? = block(ByteArrayInputStream(ByteArray(0)))

                override suspend fun getName(uriString: String): String? {
                    TODO("Not yet implemented")
                }

                override suspend fun getSize(uriString: String): Bytes? {
                    TODO("Not yet implemented")
                }

                override suspend fun getMimeType(uriString: String): String? {
                    TODO("Not yet implemented")
                }

                override suspend fun getLastModified(uriString: String): TimestampMs? {
                    TODO("Not yet implemented")
                }

                override suspend fun getUriInfo(uriString: String): UriResolver.UriInfo? {
                    TODO("Not yet implemented")
                }
            },
            getNodeKey = getNodeKey,
            getContentHash = getContentHash,
            configurationProvider = object : ConfigurationProvider {
                override val host: String = ""
                override val baseUrl: String = ""
                override val appVersionHeader: String = ""
            },
        )
    }

    @Test
    fun success() = runTest {
        backupFileRepository.insertFiles(
            listOf(backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE)),
        )
        backupDuplicateRepository.insertDuplicates(
            listOf(
                backupDuplicate(
                    hash = "hash",
                    contentHash = "getContentHash(da39a3ee5e6b4b0d3255bfef95601890afd80709)",
                )
            )
        )

        val worker = backupFindDuplicatesWorker(backupFolder)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        assertEquals(
            listOf(backupFile("hash", BackupFileState.DUPLICATED)),
            backupFileRepository.getAllFiles(folderId, 0, 100),
        )
        assertEquals(
            emptyList<BackupDuplicate>(),
            backupDuplicateRepository.getAll(folderId, 0, 100),
        )
    }

    private fun backupFindDuplicatesWorker(
        backupFolder: BackupFolder,
        runAttemptCount: Int = 1,
    ): BackupCheckDuplicatesWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<BackupCheckDuplicatesWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = BackupCheckDuplicatesWorker(
                    context = appContext,
                    workerParams = workerParameters,
                    checkDuplicates = checkDuplicates,
                    addBackupError = AddBackupError(backupErrorRepository),
                )

            })
            .setInputData(
                BackupFindDuplicatesWorker.workDataOf(backupFolder)
            )
            .setRunAttemptCount(runAttemptCount)
            .build()
    }

    private fun backupDuplicate(hash: String, contentHash: String?) = BackupDuplicate(
        parentId = folderId,
        hash = hash,
        contentHash = contentHash,
        linkId = FileId(folderId.shareId, "link-id"),
        linkState = Link.State.ACTIVE,
        revisionId = "revision-id",
        clientUid = ""
    )

    private fun backupFile(hash: String, backupFileState: BackupFileState) = BackupFile(
        bucketId = 0,
        folderId = folderId,
        uriString = "uri",
        mimeType = "",
        name = "",
        hash = hash,
        size = 0.bytes,
        state = backupFileState,
        date = TimestampS(0L),
    )
}
