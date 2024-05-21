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
import me.proton.core.drive.backup.data.manager.StubbedUploadWorkManager
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.AddFolder
import me.proton.core.drive.backup.domain.usecase.SetFiles
import me.proton.core.drive.backup.domain.usecase.UploadFolder
import me.proton.core.drive.base.domain.entity.StorageInfo
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.usecase.TestGetInternalStorageInfo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class BackupUploadFolderWorkerTest {

    @get:Rule
    val driveRule = DriveRule(this)

    private lateinit var folderId: FolderId
    private val bucketId = 0

    @Inject
    lateinit var uploadFolder: UploadFolder

    @Inject
    lateinit var setFiles: SetFiles

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var getInternalStorageInfo: TestGetInternalStorageInfo

    @Inject
    lateinit var uploadWorkManager: StubbedUploadWorkManager

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
        getInternalStorageInfo.storageInfo = StorageInfo(100.GiB, 80.GiB)
        addFolder(BackupFolder(bucketId, folderId)).getOrThrow()
    }

    @Test
    fun `Given ten files when upload should only upload five of them`() = runTest {
        setFiles((0..9).map { index ->
            NullableBackupFile(
                bucketId = bucketId,
                folderId = folderId,
                uriString = "uri$index",
                state = BackupFileState.READY
            )
        }).getOrThrow()

        val worker = backupUploadFolderWorker(
            BackupFolder(
                bucketId = bucketId,
                folderId = folderId,
            ),
        )
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)


        assertEquals(
            mapOf(
                folderId to UploadBulk(
                    id = 1,
                    userId = folderId.userId,
                    volumeId = volumeId,
                    shareId = folderId.shareId,
                    parentLinkId = folderId,
                    uploadFileDescriptions = (0..9).map { index -> UploadFileDescription("uri$index") },
                    shouldDeleteSourceUri = false,
                    networkTypeProviderType = NetworkTypeProviderType.BACKUP,
                    shouldAnnounceEvent = false,
                    cacheOption = CacheOption.THUMBNAIL_DEFAULT,
                    priority = UploadFileLink.BACKUP_PRIORITY,
                    shouldBroadcastErrorMessage = false
                )
            ), uploadWorkManager.bulks
        )

    }

    private fun backupUploadFolderWorker(
        backupFolder: BackupFolder,
    ): BackupUploadFolderWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<BackupUploadFolderWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {

                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): BackupUploadFolderWorker {
                    return BackupUploadFolderWorker(
                        context = appContext,
                        workerParams = workerParameters,
                        uploadFolder = uploadFolder,
                        addBackupError = AddBackupError(BackupErrorRepositoryImpl(driveRule.db)),
                    )
                }

            })
            .setInputData(BackupUploadFolderWorker.workDataOf(backupFolder))
            .build()
    }

}
