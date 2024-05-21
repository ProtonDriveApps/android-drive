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

package me.proton.core.drive.backup.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.db.entity.BackupFileEntity
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class BackupFolderRepositoryImplGetFolderByFileUriTest {

    @get:Rule
    val driveRule = DriveRule(this)

    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder
    @Inject lateinit var repository: BackupFolderRepositoryImpl

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
        backupFolder = BackupFolder(0, folderId)
        repository.insertFolder(backupFolder)
    }

    @Test
    fun empty() = runTest {
        val folderFromFile = repository.getFolderByFileUri(userId, "uri")

        assertNull(folderFromFile)
    }

    @Test
    fun `no match`() = runTest {
        driveRule.db.backupFileDao.insertOrUpdate(
            backupFileEntity("uri1")
        )

        val folderFromFile = repository.getFolderByFileUri(userId, "uri2")

        assertNull(folderFromFile)
    }

    @Test
    fun match() = runTest {
        driveRule.db.backupFileDao.insertOrUpdate(
            backupFileEntity("uri1"),
            backupFileEntity("uri2"),
        )

        val folderFromFile = repository.getFolderByFileUri(userId, "uri1")

        assertEquals(backupFolder, folderFromFile)
    }

    private fun backupFileEntity(uriString: String) = BackupFileEntity(
        userId = userId,
        bucketId = 0,
        shareId = folderId.shareId.id,
        parentId = folderId.id,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = "",
        size = 0L,
        createTime = 0L,
        state = BackupFileState.IDLE,
        uploadPriority = UploadFileLink.BACKUP_PRIORITY,
    )
}
