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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupStateCount
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackupFileRepositoryImplStatsTest {
    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var repository: BackupFileRepositoryImpl

    private val bucketId = 0
    
    @Before
    fun setUp() {
        runTest {
            folderId = database.myDrive { }
            val backupFolderRepository = BackupFolderRepositoryImpl(database.db)
            backupFolderRepository.insertFolder(BackupFolder(bucketId, folderId))
            repository = BackupFileRepositoryImpl(database.db)
        }
    }

    @Test
    fun empty() = runTest {
        assertEquals(
            emptyList<BackupStateCount>(),
            repository.getStatsForFolder(userId, bucketId),
        )
    }

    @Test
    fun backupFileState() = runTest {
        repository.insertFiles(
            userId = userId,
            backupFiles = listOf(backupFile(0, BackupFileState.IDLE))
                    + (1..2).map { backupFile(it, BackupFileState.POSSIBLE_DUPLICATE) }
                    + (3..5).map { backupFile(it, BackupFileState.DUPLICATED) }
                    + (6..9).map { backupFile(it, BackupFileState.READY) }
                    + (10..19).map { backupFile(it, BackupFileState.COMPLETED) },
        )
        assertEquals(
            listOf(
                BackupStateCount(BackupFileState.COMPLETED, count = 10),
                BackupStateCount(BackupFileState.DUPLICATED, count = 3),
                BackupStateCount(BackupFileState.IDLE, count = 1),
                BackupStateCount(BackupFileState.POSSIBLE_DUPLICATE, count = 2),
                BackupStateCount(BackupFileState.READY, count = 4),
            ),
            repository.getStatsForFolder(userId, bucketId),
        )
    }

    @Test
    fun uploadState() = runTest {
        repository.insertFiles(
            userId = userId,
            backupFiles = (0..10).map { backupFile(it, BackupFileState.READY) },
        )
        insertLinkUploadEntity(0, UploadState.IDLE)
        (1..8).onEach { insertLinkUploadEntity(it, UploadState.UPLOADING_BLOCKS) }
        (9..10).onEach { insertLinkUploadEntity(it, UploadState.CLEANUP) }
        assertEquals(
            listOf(
                BackupStateCount(BackupFileState.READY, UploadState.CLEANUP, 2),
                BackupStateCount(BackupFileState.READY, UploadState.IDLE, 1),
                BackupStateCount(BackupFileState.READY, UploadState.UPLOADING_BLOCKS, 8),
            ),
            repository.getStatsForFolder(userId, bucketId),
        )
    }

    private fun backupFile(
        index: Int,
        state: BackupFileState,
    ) = backupFile(
        uriString = "uri$index",
        hash = "hash$index",
        state = state,
    )

    private fun backupFile(
        uriString: String = "uri",
        bucketId: Int = 0,
        hash: String = "hash",
        state: BackupFileState = BackupFileState.IDLE,
    ) = BackupFile(
        bucketId = bucketId,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = hash,
        size = 0.bytes,
        state = state,
        date = TimestampS(0L),
    )

    private suspend fun insertLinkUploadEntity(index: Int, uploadState: UploadState) {
        database.db.linkUploadDao.insert(
            LinkUploadEntity(
                id = 0,
                userId = userId,
                volumeId = volumeId,
                shareId = shareId,
                parentId = folderId.id,
                uri = "uri$index",
                name = "",
                size = 0,
                state = uploadState
            )
        )
    }
}
