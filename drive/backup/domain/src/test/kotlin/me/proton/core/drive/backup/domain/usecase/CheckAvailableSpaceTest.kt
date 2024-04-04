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

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.NoNetworkConfigurationProvider
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.user.domain.entity.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckAvailableSpaceTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var checkAvailableSpace: CheckAvailableSpace
    private lateinit var getError: GetErrors

    private lateinit var backupManager: StubbedBackupManager

    @Before
    fun setup() = runTest {
        folderId = database.myFiles { }
        val folderRepository = BackupFolderRepositoryImpl(database.db)
        val fileRepository = BackupFileRepositoryImpl(database.db)
        val errorRepository = BackupErrorRepositoryImpl(database.db)
        val addBackupError = AddBackupError(errorRepository)

        backupManager = StubbedBackupManager(folderRepository)
        val configurationProvider = object : NoNetworkConfigurationProvider {
            override val backupLeftSpace: Bytes = 10.MiB
        }
        val getAllFolders = GetAllFolders(folderRepository)
        checkAvailableSpace = CheckAvailableSpace(
            configurationProvider = configurationProvider,
            getAllFolders = getAllFolders,
            stopBackup = StopBackup(
                manager = backupManager,
                addBackupError = addBackupError,
                logBackupStats = LogBackupStats(folderRepository, fileRepository),
                announceEvent = AnnounceEvent(emptySet()),
                getAllFolders = getAllFolders,
                markAllEnqueuedAsReady = MarkAllEnqueuedAsReady(fileRepository),
            )
        )
        getError = GetErrors(errorRepository, configurationProvider)
        val addFolder = AddFolder(folderRepository)
        addFolder(BackupFolder(0, folderId)).getOrThrow()
    }

    @Test
    fun `enough space should do nothing`() = runTest {
        checkAvailableSpace(user(100.MiB.value, 0.MiB.value)).getOrThrow()

        assertFalse(backupManager.stopped)
    }

    @Test
    fun `when over the limit should stop backup`() = runTest {
        checkAvailableSpace(user(100.MiB.value, 95.MiB.value)).getOrThrow()

        assertTrue(backupManager.stopped)
        assertEquals(
            listOf(BackupError.DriveStorage()),
            getError(folderId).first(),
        )
    }

    private fun user(maxSpace: Long, usedSpace: Long) = User(
        userId = userId,
        email = "",
        name = "",
        displayName = "",
        currency = "",
        credit = 0,
        usedSpace = usedSpace,
        maxSpace = maxSpace,
        maxUpload = 0,
        role = null,
        private = false,
        services = 0,
        subscribed = 0,
        delinquent = null,
        keys = emptyList(),
        recovery = null,
        createdAtUtc = 0,
        type = null,
    )
}
