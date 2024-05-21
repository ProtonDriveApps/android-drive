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
import me.proton.core.drive.link.domain.entity.FolderId
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
class GetFolderFromFileTest {
    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var getFolderFromFile: GetFolderFromFile

    @Inject
    lateinit var setFiles: SetFiles

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
    }

    @Test
    fun empty() = runTest {
        val folderFromFile = getFolderFromFile(userId, "uri").getOrThrow()

        assertNull(folderFromFile)
    }

    @Test
    fun match() = runTest {
        val backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId,
        )
        addFolder(backupFolder).getOrThrow()
        setFiles(
            backupFiles = listOf(
                BackupFile(
                    bucketId = 0,
                    folderId = folderId,
                    uriString = "uri",
                    mimeType = "",
                    name = "",
                    hash = "",
                    size = 0.bytes,
                    state = BackupFileState.IDLE,
                    date = TimestampS(0),
                ),
            ),
        ).getOrThrow()

        val folderFromFile = getFolderFromFile(userId, "uri").getOrThrow()

        assertEquals(backupFolder, folderFromFile)
    }
}
