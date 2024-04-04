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
import me.proton.core.drive.link.domain.entity.FolderId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarkAllFailedAsReadyTest {
    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var addFolder: AddFolder
    private lateinit var setFiles: SetFiles
    private lateinit var markAllFailedAsReady: MarkAllFailedAsReady

    @Before
    fun setUp() = runTest {
        folderId = database.myFiles { }
        val folderRepository = BackupFolderRepositoryImpl(database.db)
        val fileRepository = BackupFileRepositoryImpl(database.db)
        addFolder = AddFolder(folderRepository)
        setFiles = SetFiles(fileRepository)
        markAllFailedAsReady = MarkAllFailedAsReady(fileRepository)
    }

    @Test
    fun none() = runTest {
        val backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )
        addFolder(backupFolder).getOrThrow()

        val count = markAllFailedAsReady(
            folderId = folderId,
            bucketId = 0,
            maxAttempts = 5,
        ).getOrThrow()

        assertEquals(0, count)
    }

    @Test
    fun `only failed`() = runTest {
        val backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )
        addFolder(backupFolder).getOrThrow()
        setFiles(
            listOf(
                backupFile(0, BackupFileState.READY),
                backupFile(1, BackupFileState.FAILED),
                backupFile(2, BackupFileState.FAILED),
                backupFile(3, BackupFileState.FAILED, attempts = 5),
                backupFile(4, BackupFileState.IDLE),
            )
        ).getOrThrow()

        val count = markAllFailedAsReady(
            folderId = folderId,
            bucketId = 0,
            maxAttempts = 5,
        ).getOrThrow()

        assertEquals(2, count)
    }

    private fun backupFile(
        index: Int,
        state: BackupFileState,
        attempts: Long = 0,
    ) = backupFile(
        uriString = "uri$index",
        hash = "hash$index",
        state = state,
        attempts = attempts,
    )

    private fun backupFile(
        uriString: String = "uri",
        bucketId: Int = 0,
        hash: String = "hash",
        state: BackupFileState = BackupFileState.IDLE,
        attempts: Long = 0,
    ) = BackupFile(
        bucketId = bucketId,
        folderId = folderId,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = hash,
        size = 0.bytes,
        state = state,
        date = TimestampS(0L),
        attempts = attempts,
    )

}
