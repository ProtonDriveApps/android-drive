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
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.handler.StubbedEventHandler
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.NoNetworkConfigurationProvider
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StopBackupTest {


    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var backupManager: StubbedBackupManager
    private lateinit var getErrors: GetErrors
    private lateinit var stopBackup: StopBackup
    private val handler = StubbedEventHandler()
    private lateinit var fileRepository: BackupFileRepositoryImpl

    @Before
    fun setup() = runTest {
        folderId = database.myDrive { }
        val folderRepository = BackupFolderRepositoryImpl(database.db)
        fileRepository = BackupFileRepositoryImpl(database.db)
        val errorRepository = BackupErrorRepositoryImpl(database.db)

        val addFolder = AddFolder(folderRepository)
        addFolder(
            BackupFolder(
                bucketId = 0,
                folderId = folderId
            )
        ).getOrThrow()

        backupManager = StubbedBackupManager(folderRepository)
        getErrors = GetErrors(errorRepository, NoNetworkConfigurationProvider.instance)
        stopBackup = StopBackup(
            manager = backupManager,
            addBackupError = AddBackupError(errorRepository),
            announceEvent = AnnounceEvent(setOf(handler)),
            logBackupStats = LogBackupStats(folderRepository, fileRepository),
            getAllFolders = GetAllFolders(folderRepository),
            markAllEnqueuedAsReady = MarkAllEnqueuedAsReady(fileRepository)
        )
    }

    @Test
    fun stopBackup() = runTest {
        stopBackup(folderId, BackupError.Permissions()).getOrThrow()

        assertTrue(backupManager.stopped)
        assertEquals(
            listOf(BackupError.Permissions()),
            getErrors(folderId).first()
        )
        assertEquals(
            mapOf(userId to listOf(Event.BackupStopped(folderId, Event.Backup.BackupState.FAILED_PERMISSION))),
            handler.events,
        )
    }

    @Test
    fun markAsReady() = runTest {
        fileRepository.insertFiles(
            listOf(
                backupFile(3, BackupFileState.COMPLETED),
                backupFile(2, BackupFileState.FAILED),
                backupFile(1, BackupFileState.ENQUEUED),
            )
        )

        stopBackup(folderId, BackupError.LocalStorage()).getOrThrow()

        assertTrue(backupManager.stopped)
        assertEquals(
            listOf(
                backupFile(3, BackupFileState.COMPLETED),
                backupFile(2, BackupFileState.FAILED),
                backupFile(1, BackupFileState.READY),
            ),
            fileRepository.getAllFiles(folderId, 0, 100)
        )
    }

    private fun backupFile(
        index: Int,
        state: BackupFileState,
    ) = backupFile(
        uriString = "uri$index",
        hash = "hash$index",
        state = state,
        date = TimestampS(index.toLong()),
    )

    private fun backupFile(
        uriString: String = "uri",
        bucketId: Int = 0,
        hash: String = "hash",
        state: BackupFileState = BackupFileState.IDLE,
        date: TimestampS = TimestampS(0L)
    ) = BackupFile(
        bucketId = bucketId,
        folderId = folderId,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = hash,
        size = 0.bytes,
        state = state,
        date = date,
    )
}
