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
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BackupDuplicateRepository
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.AddFolder
import me.proton.core.drive.backup.domain.usecase.CheckDuplicates
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
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
class BackupCheckDuplicatesWorkerTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder

    @Inject
    lateinit var checkDuplicates: CheckDuplicates

    @Inject
    lateinit var addBackupError: AddBackupError

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var backupFileRepository: BackupFileRepository

    @Inject
    lateinit var backupDuplicateRepository: BackupDuplicateRepository

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId
        )
        addFolder(backupFolder).getOrThrow()
        driveRule.server.getPublicAddressKeys()
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
                    contentHash = "d736aa57fdbc8e1fd4c256e7737e2bcf55b8f6b0855ae0bfe84d4c1d148f6a53",
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
                    addBackupError = addBackupError,
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
        uriString = "test://uri",
        mimeType = "",
        name = "",
        hash = hash,
        size = 0.bytes,
        state = backupFileState,
        date = TimestampS(0L),
    )
}
