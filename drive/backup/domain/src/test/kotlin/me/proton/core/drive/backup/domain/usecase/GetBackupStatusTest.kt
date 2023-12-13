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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.data.factory.UploadBlockFactoryImpl
import me.proton.core.drive.linkupload.data.repository.LinkUploadRepositoryImpl
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetBackupStatusTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var setFiles: SetFiles
    private lateinit var markAsCompleted: MarkAsCompleted
    private lateinit var markAsFailed: MarkAsFailed
    private lateinit var getBackupStatus: GetBackupStatus
    private lateinit var cleanUpCompleteBackup: CleanUpCompleteBackup
    private lateinit var linkUploadRepository: LinkUploadRepositoryImpl

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive { }
        val backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        val addFolder = AddFolder(backupFolderRepository)
        addFolder(BackupFolder(0, folderId))
        val backupFileRepository = BackupFileRepositoryImpl(database.db)
        setFiles = SetFiles(backupFileRepository)
        markAsCompleted = MarkAsCompleted(backupFileRepository)
        markAsFailed = MarkAsFailed(backupFileRepository)
        cleanUpCompleteBackup = CleanUpCompleteBackup(
            repository = backupFileRepository,
            logBackupStats = LogBackupStats(backupFolderRepository, backupFileRepository),
            announceEvent = AnnounceEvent(emptySet()),
        )
        getBackupStatus = GetBackupStatus(backupFileRepository)
        linkUploadRepository = LinkUploadRepositoryImpl(database.db, UploadBlockFactoryImpl())
    }

    @Test
    fun `Given no files then status should be completed`() =
        runTest {
            assertEquals(
                BackupStatus.Complete(totalBackupPhotos = 0),
                getBackupStatus(userId).first(),
            )
        }

    @Test
    fun `Given tree files when two are make as completed then status should be progressing two over three`() =
        runTest {
            setFiles(
                userId, listOf(
                    backupFile("uri1"),
                    backupFile("uri2"),
                    backupFile("uri3"),
                )
            )
            linkUploadRepository.insertUploadFileLinks(
                listOf(
                    uploadFileLink("uri1", folderId),
                    uploadFileLink("uri2", folderId),
                    uploadFileLink("uri3", folderId),
                )
            )
            markAsCompleted(userId, "uri1")
            markAsCompleted(userId, "uri2")

            assertEquals(
                BackupStatus.InProgress(totalBackupPhotos = 3, pendingBackupPhotos = 1),
                getBackupStatus(userId).first(),
            )
        }

    @Test
    fun `Given tree files when all are make as completed then status should be completed`() =
        runTest {
            setFiles(
                userId, listOf(
                    backupFile("uri1"),
                    backupFile("uri2"),
                    backupFile("uri3"),
                )
            )
            linkUploadRepository.insertUploadFileLinks(
                listOf(
                    uploadFileLink("uri1", folderId),
                    uploadFileLink("uri2", folderId),
                    uploadFileLink("uri3", folderId),
                )
            )
            markAsCompleted(userId, "uri1")
            markAsCompleted(userId, "uri2")
            markAsCompleted(userId, "uri3")

            assertEquals(
                BackupStatus.Complete(totalBackupPhotos = 3),
                getBackupStatus(userId).first(),
            )
        }

    @Test
    fun `Given tree files when all are make as completed and failed then status should be uncompleted`() =
        runTest {
            setFiles(
                userId, listOf(
                    backupFile("uri1"),
                    backupFile("uri2"),
                    backupFile("uri3"),
                )
            )
            linkUploadRepository.insertUploadFileLinks(
                listOf(
                    uploadFileLink("uri1", folderId),
                    uploadFileLink("uri2", folderId),
                    uploadFileLink("uri3", folderId),
                )
            )
            markAsCompleted(userId, "uri1")
            markAsCompleted(userId, "uri2")
            markAsFailed(userId, "uri3")

            assertEquals(
                BackupStatus.Uncompleted(
                    totalBackupPhotos = 3,
                    failedBackupPhotos = 1,
                ),
                getBackupStatus(userId).first(),
            )
        }

    @Test
    fun `Given not all files completed When deleteCompletedFromFolder Should do nothing`() =
        runTest {
            val backupFile1 = backupFile("uri1")
            val backupFile2 = backupFile("uri2")

            setFiles(userId, listOf(backupFile1, backupFile2)).getOrThrow()
            markAsCompleted(userId, "uri1").getOrThrow()
            cleanUpCompleteBackup(userId, folderId, 0).getOrThrow()

            assertEquals(
                BackupStatus.InProgress(totalBackupPhotos = 2, pendingBackupPhotos = 1),
                getBackupStatus(userId = userId).first(),
            )
        }

    @Test
    fun `Given all files completed When deleteCompletedFromFolder Should delete them all`() =
        runTest {
            val backupFile1 = backupFile("uri1")
            val backupFile2 = backupFile("uri2")

            setFiles(userId, listOf(backupFile1, backupFile2)).getOrThrow()
            markAsCompleted(userId, "uri1").getOrThrow()
            markAsCompleted(userId, "uri2").getOrThrow()
            cleanUpCompleteBackup(userId, folderId, 0).getOrThrow()

            assertEquals(
                BackupStatus.Complete(totalBackupPhotos = 0),
                getBackupStatus(userId = userId).first(),
            )
        }
}

private fun backupFile(uriString: String) = BackupFile(
    bucketId = 0,
    uriString = uriString,
    mimeType = "",
    name = "",
    hash = "",
    size = 0.bytes,
    state = BackupFileState.IDLE,
    date = TimestampS(0),
)

private fun uploadFileLink(uriString: String, folderId: FolderId) = UploadFileLink(
    userId = userId,
    volumeId = VolumeId(volumeId),
    shareId = folderId.shareId,
    parentLinkId = folderId,
    uriString = uriString,
    name = "",
    mimeType = "",
    networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
    priority = UploadFileLink.BACKUP_PRIORITY,
)
