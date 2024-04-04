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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.data.extension.uniqueFolderIdTag
import me.proton.core.drive.backup.data.manager.StubbedBackupManager
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BackupFolderRepository
import me.proton.core.drive.backup.domain.usecase.AddBackupError
import me.proton.core.drive.backup.domain.usecase.CleanUpCompleteBackup
import me.proton.core.drive.backup.domain.usecase.GetAllFolders
import me.proton.core.drive.backup.domain.usecase.GetFilesToBackup
import me.proton.core.drive.backup.domain.usecase.LogBackupStats
import me.proton.core.drive.backup.domain.usecase.MarkAllEnqueuedAsReady
import me.proton.core.drive.backup.domain.usecase.MarkAllFailedAsReady
import me.proton.core.drive.backup.domain.usecase.MarkAsEnqueued
import me.proton.core.drive.backup.domain.usecase.StopBackup
import me.proton.core.drive.backup.domain.usecase.UploadFolder
import me.proton.core.drive.base.domain.entity.StorageInfo
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetInternalStorageInfo
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.drivelink.data.repository.DriveLinkRepositoryImpl
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.drivelink.domain.usecase.UpdateIsAnyAncestorMarkedAsOffline
import me.proton.core.drive.drivelink.upload.domain.entity.Notifications
import me.proton.core.drive.drivelink.upload.domain.usecase.UploadFiles
import me.proton.core.drive.link.data.repository.LinkRepositoryImpl
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.usecase.HasLink
import me.proton.core.drive.linknode.data.repository.LinkNodeRepositoryImpl
import me.proton.core.drive.linknode.domain.usecase.BuildLinkNode
import me.proton.core.drive.linknode.domain.usecase.GetLinkAncestors
import me.proton.core.drive.linknode.domain.usecase.GetLinkNode
import me.proton.core.drive.linkoffline.data.repository.LinkOfflineRepositoryImpl
import me.proton.core.drive.linkoffline.domain.usecase.IsLinkOrAnyAncestorMarkedAsOffline
import me.proton.core.drive.linkupload.data.factory.UploadBlockFactoryImpl
import me.proton.core.drive.linkupload.data.repository.LinkUploadRepositoryImpl
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLinksPaged
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.usecase.GetUser
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupUploadFolderWorkerTest {


    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var folderId: FolderId

    private lateinit var repository: BackupFileRepositoryImpl
    private lateinit var backupFolderRepository: BackupFolderRepository

    private lateinit var linkUploadRepository: LinkUploadRepositoryImpl

    private val bucketId = 0

    private val getUser = mockk<GetUser>()
    private val uploadFiles = mockk<UploadFiles>(relaxed = true)
    private val getInternalStorageInfo = mockk<GetInternalStorageInfo>()
    private lateinit var cleanUpCompleteBackup: CleanUpCompleteBackup
    private lateinit var logBackupStats: LogBackupStats

    @Before
    fun setUp() = runTest {
        folderId = database.myFiles { }
        backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        backupFolderRepository.insertFolder(BackupFolder(bucketId, folderId))
        repository = BackupFileRepositoryImpl(database.db)
        linkUploadRepository = LinkUploadRepositoryImpl(database.db, UploadBlockFactoryImpl())
        logBackupStats = LogBackupStats(backupFolderRepository, repository)
        cleanUpCompleteBackup = CleanUpCompleteBackup(
            repository = repository,
            logBackupStats = logBackupStats,
            announceEvent = AnnounceEvent(emptySet()),
        )
        val user = mockk<User>()
        coEvery { getUser.invoke(userId, false) } returns user
        every { user.maxSpace } returns Long.MAX_VALUE
        every { user.usedSpace } returns 0L
        every { getInternalStorageInfo.invoke() } returns Result.success(StorageInfo(100.GiB, 10.GiB))
    }

    @Test
    fun `Given ten files when upload should only upload five of them`() = runTest {
        repository.insertFiles((0..9).map { index ->
            NullableBackupFile(
                bucketId = bucketId,
                folderId = folderId,
                uriString = "uri$index",
                state = BackupFileState.READY
            )
        })

        val worker = backupUploadFolderWorker(
            BackupFolder(
                bucketId = bucketId,
                folderId = folderId,
            ),
        )
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify {
            uploadFiles.invoke(
                folder = any(),
                uploadFileDescriptions = (0..4).map { index -> UploadFileDescription("uri$index") },
                shouldDeleteSource = false,
                notifications = Notifications.TurnedOff,
                cacheOption = CacheOption.THUMBNAIL_DEFAULT,
                background = true,
                networkTypeProviderType = NetworkTypeProviderType.BACKUP,
                shouldBroadcastErrorMessage = false,
                priority = UploadFileLink.BACKUP_PRIORITY,
                tags = listOf(folderId.uniqueFolderIdTag()),
            )
        }
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
                    val backupErrorRepository = BackupErrorRepositoryImpl(database.db)
                    val linkRepository = LinkRepositoryImpl(mockk(), database.db)
                    val linkNodeRepository = LinkNodeRepositoryImpl(database.db.linkAncestorDao)
                    val linkUploadRepository =
                        LinkUploadRepositoryImpl(database.db, UploadBlockFactoryImpl())
                    val configurationProvider = object : ConfigurationProvider {
                        override val host: String = ""
                        override val baseUrl: String = ""
                        override val appVersionHeader: String = ""
                        override val uploadLimitThreshold: Int = 5
                    }
                    return BackupUploadFolderWorker(
                        context = appContext,
                        workerParams = workerParameters,
                        uploadFolder = UploadFolder(
                            getFilesToBackup = GetFilesToBackup(repository),
                            markAllFailedAsReady = MarkAllFailedAsReady(repository),
                            stopBackup = StopBackup(
                                manager = StubbedBackupManager(),
                                addBackupError = AddBackupError(backupErrorRepository),
                                logBackupStats = logBackupStats,
                                announceEvent = AnnounceEvent(emptySet()),
                                getAllFolders = GetAllFolders(backupFolderRepository),
                                markAllEnqueuedAsReady = MarkAllEnqueuedAsReady(repository),
                            ),
                            configurationProvider = configurationProvider,
                            uploadFiles = uploadFiles,
                            cleanUpCompleteBackup = cleanUpCompleteBackup,
                            getDriveLink = GetDriveLink(
                                hasLink = HasLink(linkRepository),
                                linkRepository = linkRepository,
                                driveLinkRepository = DriveLinkRepositoryImpl(database.db.driveLinkDao),
                                getMainShare = mockk(),
                                updateIsAnyAncestorMarkedAsOffline = UpdateIsAnyAncestorMarkedAsOffline(
                                    IsLinkOrAnyAncestorMarkedAsOffline(
                                        LinkOfflineRepositoryImpl(database.db.linkOfflineDao),
                                        GetLinkNode(
                                            linkNodeRepository = linkNodeRepository,
                                            buildLinkNode = BuildLinkNode(
                                                GetLinkAncestors(linkNodeRepository)
                                            ),
                                        )
                                    )
                                )
                            ),
                            getUploadFileLinks = GetUploadFileLinksPaged(
                                linkUploadRepository,
                                configurationProvider
                            ),
                            getUser = getUser,
                            markAsEnqueued = MarkAsEnqueued(repository),
                            getInternalStorageInfo = getInternalStorageInfo,
                        ),
                        addBackupError = AddBackupError(BackupErrorRepositoryImpl(database.db)),
                    )
                }

            })
            .setInputData(BackupUploadFolderWorker.workDataOf(backupFolder))
            .build()
    }

}
