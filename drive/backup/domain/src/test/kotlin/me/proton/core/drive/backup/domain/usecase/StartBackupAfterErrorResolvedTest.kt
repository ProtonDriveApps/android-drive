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

package me.proton.core.drive.backup.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.NoNetworkConfigurationProvider
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartBackupAfterErrorResolvedTest {


    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var backupManager: StubbedBackupManager
    private lateinit var getErrors: GetErrors
    private lateinit var addBackupError: AddBackupError
    private lateinit var startBackupAfterErrorResolved: StartBackupAfterErrorResolved

    @Before
    fun setup() = runTest {
        database.myDrive { }
        val folderRepository = BackupFolderRepositoryImpl(database.db)
        val errorRepository = BackupErrorRepositoryImpl(database.db)

        backupManager = StubbedBackupManager(folderRepository)
        addBackupError = AddBackupError(errorRepository)
        getErrors = GetErrors(errorRepository, NoNetworkConfigurationProvider.instance)
        startBackupAfterErrorResolved = StartBackupAfterErrorResolved(
            startBackup = StartBackup(backupManager, AnnounceEvent(emptySet())),
            getErrors = getErrors,
            deleteAllBackupError = DeleteAllBackupError(errorRepository),
        )
    }

    @Test
    fun doNothingWithError() = runTest {
        startBackupAfterErrorResolved(userId, BackupErrorType.PERMISSION).getOrThrow()

        assertFalse(backupManager.started)
        assertEquals(
            emptyList<BackupError>(),
            getErrors(userId).first()
        )
    }

    @Test
    fun startBackupAfterErrorResolved() = runTest {
        addBackupError(userId, BackupError.Permissions()).getOrThrow()

        startBackupAfterErrorResolved(userId, BackupErrorType.PERMISSION).getOrThrow()

        assertTrue(backupManager.started)
        assertEquals(
            emptyList<BackupError>(),
            getErrors(userId).first()
        )
    }
}
