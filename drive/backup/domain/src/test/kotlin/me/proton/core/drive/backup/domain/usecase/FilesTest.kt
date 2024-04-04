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

import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.data.factory.UploadBlockFactoryImpl
import me.proton.core.drive.linkupload.data.repository.LinkUploadRepositoryImpl
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FilesTest {
    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var getFilesToBackup: GetFilesToBackup
    private lateinit var setFiles: SetFiles
    private lateinit var deleteFile: DeleteFile
    private lateinit var markAsCompleted: MarkAsCompleted
    private lateinit var markAsFailed: MarkAsFailed
    private lateinit var markAllFailedAsReady: MarkAllFailedAsReady
    private lateinit var linkUploadRepository: LinkUploadRepositoryImpl

    @Before
    fun setUp() = runTest {
        folderId = database.myFiles {}
        val backupFileRepository = BackupFileRepositoryImpl(database.db)
        val backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        getFilesToBackup = GetFilesToBackup(backupFileRepository)
        val addFolder = AddFolder(backupFolderRepository)
        addFolder(BackupFolder(1, folderId))
        addFolder(BackupFolder(2, folderId))
        setFiles = SetFiles(backupFileRepository)
        deleteFile = DeleteFile(backupFileRepository)
        markAsCompleted = MarkAsCompleted(backupFileRepository)
        markAsFailed = MarkAsFailed(backupFileRepository)
        markAllFailedAsReady = MarkAllFailedAsReady(backupFileRepository)
        linkUploadRepository = LinkUploadRepositoryImpl(database.db, UploadBlockFactoryImpl())
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
