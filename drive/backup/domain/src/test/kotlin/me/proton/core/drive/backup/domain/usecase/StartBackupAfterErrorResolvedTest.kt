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
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class StartBackupAfterErrorResolvedTest {


    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var backupManager: StubbedBackupManager
    @Inject
    lateinit var getErrors: GetErrors
    @Inject
    lateinit var addFolder: AddFolder
    @Inject
    lateinit var addBackupError: AddBackupError
    @Inject
    lateinit var startBackupAfterErrorResolved: StartBackupAfterErrorResolved

    @Before
    fun setup() = runTest {
        folderId = driveRule.db.myFiles { }
        addFolder(BackupFolder(0, folderId)).getOrThrow()
    }

    @Test
    fun doNothingWithError() = runTest {
        startBackupAfterErrorResolved(userId, BackupErrorType.PERMISSION).getOrThrow()

        assertFalse(backupManager.started)
        assertEquals(
            emptyList<BackupError>(),
            getErrors(folderId).first()
        )
    }

    @Test
    fun startBackupAfterErrorResolved() = runTest {
        addBackupError(folderId, BackupError.Permissions()).getOrThrow()

        startBackupAfterErrorResolved(userId, BackupErrorType.PERMISSION).getOrThrow()

        assertTrue(backupManager.started)
        assertEquals(
            emptyList<BackupError>(),
            getErrors(folderId).first()
        )
    }
}
