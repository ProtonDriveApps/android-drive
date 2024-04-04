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

package me.proton.core.drive.backup.domain.usecase

import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.base.domain.entity.StorageInfo
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.usecase.GetInternalStorageInfo
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UploadFolderTest {
    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder

    private lateinit var uploadFolder: UploadFolder
    private lateinit var getErrors: GetErrors

    private lateinit var backupManager: StubbedBackupManager
    private lateinit var backupFileRepository: BackupFileRepositoryImpl
    private lateinit var linkUploadRepository: LinkUploadRepositoryImpl

    private val bucketId = 0

    private lateinit var backupFile: BackupFile
    private val getUser = mockk<GetUser>()
    private val user = mockk<User>()
    private val uploadFiles = mockk<UploadFiles>(relaxed = true)
    private val getInternalStorageInfo = mockk<GetInternalStorageInfo>()

    @Before
    fun setUp() = runTest {
        folderId = database.myFiles { }
        backupFile = NullableBackupFile(
            bucketId = bucketId,
            folderId = folderId,
            uriString = "uri",
            state = BackupFileState.READY,
        )
        val backupErrorRepository = BackupErrorRepositoryImpl(database.db)
        val backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        backupManager = StubbedBackupManager(backupFolderRepository)
        val addFolder = AddFolder(backupFolderRepository)
        backupFolder = BackupFolder(bucketId, folderId)
        addFolder(backupFolder).getOrThrow()
        backupFileRepository = BackupFileRepositoryImpl(database.db)
        val linkRepository = LinkRepositoryImpl(mockk(), database.db)
        val linkNodeRepository = LinkNodeRepositoryImpl(database.db.linkAncestorDao)
        linkUploadRepository = LinkUploadRepositoryImpl(database.db, UploadBlockFactoryImpl())
        val configurationProvider = object : ConfigurationProvider {
            override val host: String = ""
            override val baseUrl: String = ""
            override val appVersionHeader: String = ""
            override val uploadLimitThreshold: Int = 5
        }
        getErrors = GetErrors(backupErrorRepository, configurationProvider)
        val logBackupStats = LogBackupStats(backupFolderRepository, backupFileRepository)
        uploadFolder = UploadFolder(
            getFilesToBackup = GetFilesToBackup(backupFileRepository),
            markAllFailedAsReady = MarkAllFailedAsReady(backupFileRepository),
            stopBackup = StopBackup(
                manager = backupManager,
                addBackupError = AddBackupError(backupErrorRepository),
                logBackupStats = logBackupStats,
                announceEvent = AnnounceEvent(emptySet()),
                getAllFolders = GetAllFolders(backupFolderRepository),
                markAllEnqueuedAsReady = MarkAllEnqueuedAsReady(backupFileRepository),
            ),
            configurationProvider = configurationProvider,
            uploadFiles = uploadFiles,
            cleanUpCompleteBackup = CleanUpCompleteBackup(
                repository = backupFileRepository,
                logBackupStats = logBackupStats,
                announceEvent = AnnounceEvent(emptySet()),
            ),
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
                configurationProvider,
            ),
            getUser = getUser,
            markAsEnqueued = MarkAsEnqueued(backupFileRepository),
            getInternalStorageInfo = getInternalStorageInfo,
        )

        coEvery { getUser.invoke(userId, false) } returns user
        every { user.maxSpace } returns Long.MAX_VALUE
        every { user.usedSpace } returns 0L
        every { getInternalStorageInfo.invoke() } returns Result.success(StorageInfo(100.GiB, 10.GiB))
    }

    @Test
    fun `Given no file when upload should do nothing`() = runTest {

        uploadFolder(backupFolder).getOrThrow()

        verify { uploadFiles wasNot called }
    }

    @Test
    fun `Given one file when upload should one file`() = runTest {
        backupFileRepository.insertFiles(listOf(backupFile))

        uploadFolder(backupFolder).getOrThrow()

        coVerify {
            uploadFiles.invoke(
                folder = any(),
                uploadFileDescriptions = listOf(UploadFileDescription("uri")),
                shouldDeleteSource = false,
                notifications = Notifications.TurnedOff,
                cacheOption = CacheOption.THUMBNAIL_DEFAULT,
                background = true,
                networkTypeProviderType = NetworkTypeProviderType.BACKUP,
                priority = UploadFileLink.BACKUP_PRIORITY,
                shouldBroadcastErrorMessage = false,
            )
        }
    }

    @Test
    fun `Given one file already uploading when upload should do nothing`() = runTest {
        backupFileRepository.insertFiles(listOf(backupFile))

        linkUploadRepository.insertUploadFileLink(
            uploadFileLink("uri")
        )

        uploadFolder(backupFolder).getOrThrow()

        verify { uploadFiles wasNot called }
    }
    @Test
    fun `Given one file already enqueued when upload should do nothing`() = runTest {
        backupFileRepository.insertFiles(
            listOf(backupFile.copy(state = BackupFileState.ENQUEUED))
        )

        uploadFolder(backupFolder).getOrThrow()

        verify { uploadFiles wasNot called }
    }

    @Test
    fun `Given one file already uploaded when upload should delete it`() = runTest {
        backupFileRepository.insertFiles(
            listOf(backupFile.copy(state = BackupFileState.COMPLETED))
        )

        uploadFolder(backupFolder).getOrThrow()

        verify { uploadFiles wasNot called }
    }

    @Test
    fun `Given one failed file when upload should one file`() = runTest {
        backupFileRepository.insertFiles(
            listOf(backupFile.copy(state = BackupFileState.FAILED)),
        )

        uploadFolder(backupFolder).getOrThrow()

        coVerify {
            uploadFiles.invoke(
                folder = any(),
                uploadFileDescriptions = listOf(UploadFileDescription("uri")),
                shouldDeleteSource = false,
                notifications = Notifications.TurnedOff,
                cacheOption = CacheOption.THUMBNAIL_DEFAULT,
                background = true,
                networkTypeProviderType = NetworkTypeProviderType.BACKUP,
                priority = UploadFileLink.BACKUP_PRIORITY,
                shouldBroadcastErrorMessage = false,
            )
        }
    }

    @Test
    fun `Given one failed file with max attempts when upload should do nothing`() = runTest {
        backupFileRepository.insertFiles(
            listOf(backupFile.copy(state = BackupFileState.FAILED, attempts = 5)),
        )

        uploadFolder(backupFolder).getOrThrow()

        verify { uploadFiles wasNot called }
    }

    @Test
    fun `Given ten files when upload should only upload five of them`() = runTest {
        backupFileRepository.insertFiles((0..9).map { index ->
            NullableBackupFile(
                bucketId = bucketId,
                folderId = folderId,
                uriString = "uri$index",
                state = BackupFileState.READY,
            )
        })

        uploadFolder(backupFolder).getOrThrow()

        coVerify {
            uploadFiles.invoke(
                folder = any(),
                uploadFileDescriptions = (0..4).map { index -> UploadFileDescription("uri$index") },
                shouldDeleteSource = false,
                notifications = Notifications.TurnedOff,
                cacheOption = CacheOption.THUMBNAIL_DEFAULT,
                background = true,
                networkTypeProviderType = NetworkTypeProviderType.BACKUP,
                priority = UploadFileLink.BACKUP_PRIORITY,
                shouldBroadcastErrorMessage = false,
            )
        }
    }

    @Test
    fun `Given ten files and two uploading when upload should only upload two of them`() = runTest {
        backupFileRepository.insertFiles((0..9).map { index ->
            NullableBackupFile(
                bucketId = bucketId,
                folderId = folderId,
                uriString = "uri$index",
                state = BackupFileState.READY,
            )
        })

        linkUploadRepository.insertUploadFileLinks(
            listOf(
                uploadFileLink("uri0"),
                uploadFileLink("uri1"),
            )
        )

        uploadFolder(backupFolder).getOrThrow()

        coVerify {
            uploadFiles.invoke(
                folder = any(),
                uploadFileDescriptions = (2..4).map { index -> UploadFileDescription("uri$index") },
                shouldDeleteSource = false,
                notifications = Notifications.TurnedOff,
                cacheOption = CacheOption.THUMBNAIL_DEFAULT,
                background = true,
                networkTypeProviderType = NetworkTypeProviderType.BACKUP,
                priority = UploadFileLink.BACKUP_PRIORITY,
                shouldBroadcastErrorMessage = false,
            )
        }
    }

    @Test
    fun `Given ten files of 5MiB when upload should only upload five of them`() = runTest {
        every { user.maxSpace } returns 50.MiB.value
        every { user.usedSpace } returns 0L
        backupFileRepository.insertFiles((0..9).map { index ->
            NullableBackupFile(
                bucketId = bucketId,
                folderId = folderId,
                uriString = "uri$index",
                size = 5.MiB,
                state = BackupFileState.READY,
            )
        })

        uploadFolder(backupFolder).getOrThrow()

        coVerify {
            uploadFiles.invoke(
                folder = any(),
                uploadFileDescriptions = (0..4).map { index -> UploadFileDescription("uri$index") },
                shouldDeleteSource = false,
                notifications = Notifications.TurnedOff,
                cacheOption = CacheOption.THUMBNAIL_DEFAULT,
                background = true,
                networkTypeProviderType = NetworkTypeProviderType.BACKUP,
                priority = UploadFileLink.BACKUP_PRIORITY,
                shouldBroadcastErrorMessage = false,
            )
        }
    }

    @Test
    fun `Given a file too big for user space when upload should stop backup`() = runTest {
        every { user.maxSpace } returns 50.MiB.value
        every { user.usedSpace } returns 0L
        backupFileRepository.insertFiles(
            listOf(
                NullableBackupFile(
                    bucketId = bucketId,
                    folderId = folderId,
                    uriString = "uri",
                    size = 100.MiB,
                    state = BackupFileState.READY,
                )
            )
        )

        uploadFolder(backupFolder).getOrThrow()

        assertTrue(backupManager.stopped)
        assertEquals(
            listOf(BackupError.DriveStorage()),
            getErrors(folderId).first(),
        )
    }

    private fun uploadFileLink(uriString: String) = UploadFileLink(
        userId = userId,
        volumeId = volumeId,
        shareId = folderId.shareId,
        parentLinkId = folderId,
        uriString = uriString,
        name = "",
        mimeType = "",
        networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
        priority = UploadFileLink.USER_PRIORITY,
    )
}
