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

package me.proton.core.drive.backup.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.shareId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.data.db.entity.LinkUploadEntity
import me.proton.core.drive.linkupload.domain.entity.UploadState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupFileRepositoryImplTest {
    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var rootMainId: FolderId
    private lateinit var rootPhotoId: FolderId

    private lateinit var repository: BackupFileRepositoryImpl

    private val bucketId = 0

    @Before
    fun setUp() = runTest {
        rootPhotoId = database.photo {}
        rootMainId = database.myDrive {}
        val backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        backupFolderRepository.insertFolder(BackupFolder(bucketId, rootPhotoId))
        backupFolderRepository.insertFolder(BackupFolder(1, rootMainId))
        repository = BackupFileRepositoryImpl(database.db)
    }

    @Test
    fun `Given empty table when get all files then returns an empty list`() = runTest {
        assertEquals(
            listOf<BackupFile>(),
            repository.getAllFiles(folderId = rootPhotoId, fromIndex = 0, count = 1),
        )
    }

    @Test
    fun `Given one file when get all files then returns this media`() = runTest {
        val backupFiles = listOf(
            rootPhotoId.backupFile(bucketId = bucketId)
        )
        repository.insertFiles(backupFiles)

        assertEquals(
            backupFiles,
            repository.getAllFiles(folderId = rootPhotoId, fromIndex = 0, count = 1),
        )
    }

    @Test
    fun `Given one file in folder when get files from folder then returns this media`() {
        runTest {
            val idle = rootPhotoId.backupFile(
                uriString = "uri1",
                bucketId = bucketId,
                state = BackupFileState.IDLE
            )
            val ready = rootPhotoId.backupFile(
                uriString = "uri2",
                bucketId = bucketId,
                state = BackupFileState.READY
            )
            repository.insertFiles(listOf(idle, ready))

            assertEquals(
                listOf(ready),
                repository.getFilesToBackup(
                    folderId = rootPhotoId,
                    bucketId = bucketId,
                    maxAttempts = 5,
                    fromIndex = 0,
                    count = 1
                ),
            )
        }
    }

    @Test
    fun `Given one file in folder with upload when get files from folder then returns nothing`() =
        runTest {
            val backupFiles = listOf(
                rootPhotoId.backupFile(uriString = "uri", bucketId = bucketId)
            )
            repository.insertFiles(backupFiles)
            insertLinkUploadEntity("uri")

            assertEquals(
                emptyList<BackupFile>(),
                repository.getFilesToBackup(
                    folderId = rootPhotoId,
                    bucketId = bucketId,
                    maxAttempts = 5,
                    fromIndex = 0,
                    count = 1
                ),
            )
        }

    @Test
    fun `Given one file completed in folder when get files from folder then returns nothing`() =
        runTest {
            val backupFiles = listOf(
                rootPhotoId.backupFile(uriString = "uri", bucketId = bucketId)
            )
            repository.insertFiles(backupFiles)
            repository.markAsCompleted(rootPhotoId, "uri")

            assertEquals(
                emptyList<BackupFile>(),
                repository.getFilesToBackup(
                    folderId = rootPhotoId,
                    bucketId = bucketId,
                    maxAttempts = 5,
                    fromIndex = 0,
                    count = 1
                ),
            )
        }

    @Test
    fun `Given one file in folder when delete it then files should be empty`() =
        runTest {
            val file = rootPhotoId.backupFile()

            repository.insertFiles(listOf(file))
            repository.delete(rootPhotoId, file.uriString)

            assertEquals(
                emptyList<BackupFile>(),
                repository.getAllFiles(folderId = rootPhotoId, fromIndex = 0, count = 1),
            )
        }

    @Test
    fun `Given no files then status should be completed`() =
        runTest {
            assertEquals(
                BackupStatus.Complete(totalBackupPhotos = 0),
                repository.getBackupStatus(rootPhotoId).first(),
            )
        }

    @Test
    fun `Given files with all states then status should be progressing three over four`() =
        runTest {
            repository.insertFiles(
                listOf(
                    rootPhotoId.backupFile("uri1", state = BackupFileState.IDLE),
                    rootPhotoId.backupFile("uri2", state = BackupFileState.POSSIBLE_DUPLICATE),
                    rootPhotoId.backupFile("uri3", state = BackupFileState.DUPLICATED),
                    rootPhotoId.backupFile("uri4", state = BackupFileState.READY),
                    rootPhotoId.backupFile("uri5", state = BackupFileState.COMPLETED),
                    rootMainId.backupFile("uri6", state = BackupFileState.COMPLETED, bucketId = 1),
                )
            )

            assertEquals(
                BackupStatus.InProgress(totalBackupPhotos = 4, pendingBackupPhotos = 3),
                repository.getBackupStatus(rootPhotoId).first(),
            )
        }

    @Test
    fun `Given tree files when all are mark as completed then status should be completed`() =
        runTest {
            repository.insertFiles(
                listOf(
                    rootPhotoId.backupFile("uri1"),
                    rootPhotoId.backupFile("uri2"),
                    rootPhotoId.backupFile("uri3"),
                )
            )
            repository.markAsCompleted(rootPhotoId, "uri1")
            repository.markAsCompleted(rootPhotoId, "uri2")
            repository.markAsCompleted(rootPhotoId, "uri3")

            assertEquals(
                BackupStatus.Complete(totalBackupPhotos = 3),
                repository.getBackupStatus(rootPhotoId).first(),
            )
        }

    @Test
    fun `Given tree files when all are mark as completed and failed then status should be failed`() =
        runTest {
            repository.insertFiles(
                listOf(
                    rootPhotoId.backupFile("uri1"),
                    rootPhotoId.backupFile("uri2"),
                    rootPhotoId.backupFile("uri3"),
                )
            )
            repository.markAsCompleted(rootPhotoId, "uri1")
            repository.markAsCompleted(rootPhotoId, "uri2")
            repository.markAsFailed(rootPhotoId, "uri3")

            assertEquals(
                BackupStatus.Uncompleted(
                    totalBackupPhotos = 3,
                    failedBackupPhotos = 1,
                ),
                repository.getBackupStatus(rootPhotoId).first(),
            )
        }
    @Test
    fun `Given tree files when all are mark as completed and failed then count should be 1 for failed`() =
        runTest {
            repository.insertFiles(
                listOf(
                    rootPhotoId.backupFile("uri1"),
                    rootPhotoId.backupFile("uri2"),
                    rootPhotoId.backupFile("uri3"),
                )
            )
            repository.markAsCompleted(rootPhotoId, "uri1")
            repository.markAsFailed(rootPhotoId, "uri2")
            repository.markAsFailed(rootPhotoId, "uri3")

            assertEquals(
                2,
                repository.getCountByState(rootPhotoId, BackupFileState.FAILED),
            )
        }

    @Test
    fun `Given three files when they are marked then should be able to query them`() =
        runTest {
            repository.insertFiles(
                listOf(
                    rootPhotoId.backupFile(index = 1, state = BackupFileState.IDLE),
                    rootPhotoId.backupFile(index = 2, state = BackupFileState.IDLE),
                    rootPhotoId.backupFile(index = 3, state = BackupFileState.IDLE),
                )
            )
            repository.markAs(rootPhotoId, listOf("hash1", "hash2"), BackupFileState.READY)
            repository.markAs(rootPhotoId, listOf("hash3", "hash4"), BackupFileState.POSSIBLE_DUPLICATE)

            assertEquals(
                listOf(
                    rootPhotoId.backupFile(index = 1, state = BackupFileState.READY),
                    rootPhotoId.backupFile(index = 2, state = BackupFileState.READY),
                ),
                repository.getAllInFolderWithState(
                    folderId = rootPhotoId,
                    bucketId = bucketId,
                    state = BackupFileState.READY,
                    count = 100,
                ),
            )

            assertEquals(
                listOf(
                    rootPhotoId.backupFile(index = 3, state = BackupFileState.POSSIBLE_DUPLICATE),
                ),
                repository.getAllInFolderWithState(
                    folderId = rootPhotoId,
                    bucketId = bucketId,
                    state = BackupFileState.POSSIBLE_DUPLICATE,
                    count = 100,
                ),
            )
        }

    @Test
    fun `Given 100000 files when they are marked as enqueued then should be able to query them`() =
        runTest {
            val backupFiles = (1..100000).map { index ->
                rootPhotoId.backupFile(index = index, state = BackupFileState.READY)
            }
            repository.insertFiles(backupFiles)
            repository.markAsEnqueued(rootPhotoId, backupFiles.map { it.uriString })

            assertEquals(
                backupFiles.map { it.copy(state = BackupFileState.ENQUEUED) },
                repository.getAllInFolderWithState(
                    folderId = rootPhotoId,
                    bucketId = 0,
                    state = BackupFileState.ENQUEUED,
                    count = 100000,
                ),
            )
        }

    private fun FolderId.backupFile(
        index: Int,
        state: BackupFileState,
    ) = backupFile(
        uriString = "uri$index",
        hash = "hash$index",
        state = state,
    )

    private fun FolderId.backupFile(
        uriString: String = "uri",
        bucketId: Int = 0,
        hash: String = "hash",
        state: BackupFileState = BackupFileState.IDLE,
    ) = BackupFile(
        bucketId = bucketId,
        folderId = this,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = hash,
        size = 0.bytes,
        state = state,
        date = TimestampS(0L),
    )

    private suspend fun insertLinkUploadEntity(uri: String) {
        database.db.linkUploadDao.insert(
            LinkUploadEntity(
                id = 0,
                userId = userId,
                volumeId = volumeId,
                shareId = shareId,
                parentId = rootPhotoId.id,
                uri = uri,
                name = "",
                size = 0,
                state = UploadState.IDLE
            )
        )
    }
}
