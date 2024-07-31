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

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetBackupStatusTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder

    @Inject
    lateinit var setFiles: SetFiles

    @Inject
    lateinit var markAsCompleted: MarkAsCompleted

    @Inject
    lateinit var markAsFailed: MarkAsFailed

    @Inject
    lateinit var getBackupStatus: GetBackupStatus

    @Inject
    lateinit var cleanUpCompleteBackup: CleanUpCompleteBackup

    @Inject
    lateinit var linkUploadRepository: LinkUploadRepository

    @Inject
    lateinit var addFolder: AddFolder

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
        backupFolder = BackupFolder(0, folderId)
        addFolder(backupFolder).getOrThrow()
    }

    @Test
    fun `Given no files then status should be completed`() =
        runTest {
            assertEquals(
                BackupStatus.Complete(totalBackupPhotos = 0),
                getBackupStatus(folderId).first(),
            )
        }

    @Test
    fun `Given tree files when two are make as completed then status should be progressing two over three`() =
        runTest {
            setFiles(
                listOf(
                    backupFile("uri1"),
                    backupFile("uri2"),
                    backupFile("uri3"),
                )
            ).getOrThrow()
            linkUploadRepository.insertUploadFileLinks(
                listOf(
                    uploadFileLink("uri1", folderId),
                    uploadFileLink("uri2", folderId),
                    uploadFileLink("uri3", folderId),
                )
            )
            markAsCompleted(folderId, "uri1").getOrThrow()
            markAsCompleted(folderId, "uri2").getOrThrow()

            assertEquals(
                BackupStatus.InProgress(totalBackupPhotos = 3, pendingBackupPhotos = 1),
                getBackupStatus(folderId).first(),
            )
        }

    @Test
    fun `Given tree files when all are make as completed then status should be completed`() =
        runTest {
            setFiles(
                listOf(
                    backupFile("uri1"),
                    backupFile("uri2"),
                    backupFile("uri3"),
                )
            ).getOrThrow()
            linkUploadRepository.insertUploadFileLinks(
                listOf(
                    uploadFileLink("uri1", folderId),
                    uploadFileLink("uri2", folderId),
                    uploadFileLink("uri3", folderId),
                )
            )
            markAsCompleted(folderId, "uri1").getOrThrow()
            markAsCompleted(folderId, "uri2").getOrThrow()
            markAsCompleted(folderId, "uri3").getOrThrow()

            assertEquals(
                BackupStatus.Complete(totalBackupPhotos = 3),
                getBackupStatus(folderId).first(),
            )
        }

    @Test
    fun `Given tree files when all are make as completed and failed then status should be uncompleted`() =
        runTest {
            setFiles(
                listOf(
                    backupFile("uri1"),
                    backupFile("uri2"),
                    backupFile("uri3"),
                )
            ).getOrThrow()
            linkUploadRepository.insertUploadFileLinks(
                listOf(
                    uploadFileLink("uri1", folderId),
                    uploadFileLink("uri2", folderId),
                    uploadFileLink("uri3", folderId),
                )
            )
            markAsCompleted(folderId, "uri1").getOrThrow()
            markAsCompleted(folderId, "uri2").getOrThrow()
            markAsFailed(folderId, "uri3").getOrThrow()

            assertEquals(
                BackupStatus.Uncompleted(
                    totalBackupPhotos = 3,
                    failedBackupPhotos = 1,
                ),
                getBackupStatus(folderId).first(),
            )
        }

    @Test
    fun `Given not all files completed When deleteCompletedFromFolder Should do nothing`() =
        runTest {
            val backupFile1 = backupFile("uri1")
            val backupFile2 = backupFile("uri2")

            setFiles(listOf(backupFile1, backupFile2)).getOrThrow()
            markAsCompleted(folderId, "uri1").getOrThrow()
            cleanUpCompleteBackup(backupFolder).getOrThrow()

            assertEquals(
                BackupStatus.InProgress(totalBackupPhotos = 2, pendingBackupPhotos = 1),
                getBackupStatus(folderId).first(),
            )
        }

    @Test
    fun `Given all files completed When deleteCompletedFromFolder Should delete them all`() =
        runTest {
            val backupFile1 = backupFile("uri1")
            val backupFile2 = backupFile("uri2")

            setFiles(listOf(backupFile1, backupFile2)).getOrThrow()
            markAsCompleted(folderId, "uri1").getOrThrow()
            markAsCompleted(folderId, "uri2").getOrThrow()
            cleanUpCompleteBackup(backupFolder).getOrThrow()

            assertEquals(
                BackupStatus.Complete(totalBackupPhotos = 0),
                getBackupStatus(folderId).first(),
            )
        }

    private fun backupFile(uriString: String) = BackupFile(
        bucketId = 0,
        folderId = folderId,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = "",
        size = 0.bytes,
        state = BackupFileState.READY,
        date = TimestampS(0),
    )
}

private fun uploadFileLink(uriString: String, folderId: FolderId) = UploadFileLink(
    userId = userId,
    volumeId = volumeId,
    shareId = folderId.shareId,
    parentLinkId = folderId,
    uriString = uriString,
    name = "",
    mimeType = "",
    networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
    priority = UploadFileLink.BACKUP_PRIORITY,
)
