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
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
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
class FilesTest {
    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var getFilesToBackup: GetFilesToBackup

    @Inject
    lateinit var setFiles: SetFiles

    @Inject
    lateinit var deleteFile: DeleteFile

    @Inject
    lateinit var markAsCompleted: MarkAsCompleted

    @Inject
    lateinit var markAsFailed: MarkAsFailed

    @Inject
    lateinit var markAllFailedAsReady: MarkAllFailedAsReady

    @Inject
    lateinit var linkUploadRepository: LinkUploadRepository

    @Inject
    lateinit var addFolder: AddFolder

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles {}
        addFolder(BackupFolder(1, folderId))
        addFolder(BackupFolder(2, folderId))
    }


    @Test
    fun empty() = runTest {
        assertEquals(
            emptyList<BackupFile>(),
            getFilesToBackup(
                folderId = folderId,
                bucketId = 0,
                maxAttempts = 5,
                fromIndex = 0,
                count = 1
            ).getOrThrow()
        )
    }

    @Test
    fun set() = runTest {
        val backupFile1 = backupFile(1, "uri1")
        val backupFile2 = backupFile(2, "uri2")

        setFiles(listOf(backupFile1, backupFile2)).getOrThrow()

        assertEquals(
            listOf(backupFile1),
            getFilesToBackup(
                folderId = folderId,
                bucketId = 1,
                maxAttempts = 5,
                fromIndex = 0,
                count = 2
            ).getOrThrow()
        )
    }

    @Test
    fun toBackup() = runTest {
        val backupFile1 = backupFile(1, "uri1")
        val backupFile2 = backupFile(1, "uri2")

        setFiles(listOf(backupFile1, backupFile2)).getOrThrow()

        linkUploadRepository.insertUploadFileLink(
            UploadFileLink(
                userId = userId,
                volumeId = volumeId,
                shareId = folderId.shareId,
                parentLinkId = folderId,
                uriString = "uri1",
                name = "",
                mimeType = "",
                networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
                priority = UploadFileLink.BACKUP_PRIORITY,
            )
        )

        assertEquals(
            listOf(backupFile2),
            getFilesToBackup(
                folderId = folderId,
                bucketId = 1,
                maxAttempts = 5,
                fromIndex = 0,
                count = 2
            ).getOrThrow()
        )
    }

    @Test
    fun delete() = runTest {
        val backupFile1 = backupFile(1, "uri1")
        val backupFile2 = backupFile(1, "uri2")

        setFiles(listOf(backupFile1, backupFile2)).getOrThrow()

        deleteFile(folderId, "uri1").getOrThrow()

        assertEquals(
            listOf(backupFile2),
            getFilesToBackup(
                folderId = folderId,
                bucketId = 1,
                maxAttempts = 5,
                fromIndex = 0,
                count = 2
            ).getOrThrow()
        )
    }

    @Test
    fun markAsCompleted() = runTest {
        val backupFile1 = backupFile(1, "uri1")
        val backupFile2 = backupFile(1, "uri2")

        setFiles(listOf(backupFile1, backupFile2)).getOrThrow()
        markAsCompleted(folderId, "uri1").getOrThrow()

        assertEquals(
            listOf(backupFile2),
            getFilesToBackup(
                folderId = folderId,
                bucketId = 1,
                maxAttempts = 5,
                fromIndex = 0,
                count = 2
            ).getOrThrow(),
        )
    }

    @Test
    fun markAsFailed() = runTest {
        val backupFile1 = backupFile(1, "uri1")
        val backupFile2 = backupFile(1, "uri2")

        setFiles(listOf(backupFile1, backupFile2)).getOrThrow()
        markAsFailed(folderId, "uri1").getOrThrow()

        assertEquals(
            listOf(backupFile2),
            getFilesToBackup(
                folderId = folderId,
                bucketId = 1,
                maxAttempts = 5,
                fromIndex = 0,
                count = 2
            ).getOrThrow(),
        )
    }

    @Test
    fun markAsFailed_retry() = runTest {
        val backupFile1 = backupFile(1, "uri1")
        val backupFile2 = backupFile(1, "uri2")

        setFiles(listOf(backupFile1, backupFile2)).getOrThrow()
        markAsFailed(folderId, "uri1").getOrThrow()
        markAllFailedAsReady(folderId, 1, 5).getOrThrow()

        assertEquals(
            listOf(backupFile1.copy(attempts = 1), backupFile2),
            getFilesToBackup(
                folderId = folderId,
                bucketId = 1,
                maxAttempts = 5,
                fromIndex = 0,
                count = 2
            ).getOrThrow(),
        )
    }

    @Test
    fun markAsFailed_max_retry() = runTest {
        val backupFile1 = backupFile(1, "uri1")
        val backupFile2 = backupFile(1, "uri2")

        setFiles(listOf(backupFile1, backupFile2)).getOrThrow()
        markAsFailed(folderId, "uri1").getOrThrow()
        markAsFailed(folderId, "uri1").getOrThrow()
        markAsFailed(folderId, "uri1").getOrThrow()
        markAsFailed(folderId, "uri1").getOrThrow()
        markAsFailed(folderId, "uri1").getOrThrow()
        markAllFailedAsReady(folderId, 1, 5).getOrThrow()

        assertEquals(
            listOf(backupFile2),
            getFilesToBackup(
                folderId = folderId,
                bucketId = 1,
                maxAttempts = 5,
                fromIndex = 0,
                count = 2
            ).getOrThrow(),
        )
    }

    @Test
    fun `set should not override status`() = runTest {
        val backupFile1 = backupFile(1, "uri1")
        val backupFile2 = backupFile(1, "uri2")

        setFiles(listOf(backupFile1, backupFile2)).getOrThrow()
        markAsCompleted(folderId, "uri1").getOrThrow()
        setFiles(listOf(backupFile1, backupFile2)).getOrThrow()

        assertEquals(
            listOf(backupFile2),
            getFilesToBackup(
                folderId = folderId,
                bucketId = 1,
                maxAttempts = 5,
                fromIndex = 0,
                count = 2
            ).getOrThrow(),
        )
    }

    private fun backupFile(
        bucketId: Int,
        uriString: String,
    ) = BackupFile(
        bucketId = bucketId,
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
