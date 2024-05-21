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
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FolderId
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
class FilesResolveTest {
    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var getAllFailedFiles: GetAllFailedFiles

    @Inject
    lateinit var setFiles: SetFiles

    @Inject
    lateinit var deleteAllFailedFiles: DeleteAllFailedFiles

    @Inject
    lateinit var markAllFailedAsReady: MarkAllFailedAsReady

    @Inject
    lateinit var addFolder: AddFolder

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles {}
        addFolder(BackupFolder(0, folderId)).getOrThrow()
    }

    @Test
    fun empty() = runTest {
        assertEquals(
            emptyList<BackupFile>(),
            getAllFailedFiles(folderId).first(),
        )
    }

    @Test
    fun failed() = runTest {
        val backupFiles = listOf(
            backupFile(1, BackupFileState.FAILED),
            backupFile(2, BackupFileState.COMPLETED),
        )
        setFiles(backupFiles).getOrThrow()
        assertEquals(
            listOf(backupFile(1, BackupFileState.FAILED)),
            getAllFailedFiles(folderId).first(),
        )
    }

    @Test
    fun skipped() = runTest {
        val backupFiles = listOf(
            backupFile(1, BackupFileState.FAILED),
            backupFile(2, BackupFileState.COMPLETED),
        )
        setFiles(backupFiles).getOrThrow()
        deleteAllFailedFiles(folderId).getOrThrow()
        assertEquals(
            emptyList<BackupFile>(),
            getAllFailedFiles(folderId).first(),
        )
    }

    @Test
    fun retry() = runTest {
        val backupFiles = listOf(
            backupFile(1, BackupFileState.FAILED),
            backupFile(2, BackupFileState.COMPLETED),
        )
        setFiles(backupFiles).getOrThrow()
        markAllFailedAsReady(folderId, 0, 5).getOrThrow()
        assertEquals(
            emptyList<BackupFile>(),
            getAllFailedFiles(folderId).first(),
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
        folderId = folderId,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = hash,
        size = 0.bytes,
        state = state,
        date = TimestampS(0L),
    )
}
