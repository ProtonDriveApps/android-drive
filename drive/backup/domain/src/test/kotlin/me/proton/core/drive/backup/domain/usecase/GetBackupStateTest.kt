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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.manager.BackupPermissionsManagerImpl
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.manager.StubbedBackupConnectivityManager
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.NoNetworkConfigurationProvider
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetBackupStateTest {

    @get:Rule
    val database = DriveDatabaseRule()

    private val appContext = ApplicationProvider.getApplicationContext<Application>()
    private lateinit var addFolder: AddFolder
    private lateinit var deleteFolders: DeleteFolders
    private lateinit var setFiles: SetFiles
    private lateinit var markAsCompleted: MarkAsCompleted
    private lateinit var markAsFailed: MarkAsFailed
    private lateinit var backupState: Flow<BackupState>

    private lateinit var backupManager: StubbedBackupManager
    private val permissionsManager: BackupPermissionsManager =
        BackupPermissionsManagerImpl(appContext)
    private val connectivityManager = StubbedBackupConnectivityManager


    @Before
    fun setUp() = runTest {
        val folderRepository = BackupFolderRepositoryImpl(database.db)
        val fileRepository = BackupFileRepositoryImpl(database.db)
        val errorRepository = BackupErrorRepositoryImpl(database.db)

        addFolder = AddFolder(folderRepository)
        deleteFolders = DeleteFolders(folderRepository)
        setFiles = SetFiles(fileRepository)
        markAsCompleted = MarkAsCompleted(fileRepository)
        markAsFailed = MarkAsFailed(fileRepository)
        backupManager = StubbedBackupManager(folderRepository)
        addFolder = AddFolder(folderRepository)
        deleteFolders = DeleteFolders(folderRepository)
        permissionsManager.onPermissionChanged(BackupPermissions.Granted)

        val getBackupState = GetBackupState(
            getBackupStatus = GetBackupStatus(fileRepository),
            backupManager = backupManager,
            permissionsManager = permissionsManager,
            connectivityManager = connectivityManager,
            getErrors = GetErrors(errorRepository, NoNetworkConfigurationProvider.instance),
        )

        backupState = getBackupState(userId)
    }

    @Test
    fun `blank backup state`() = runTest {
        database.myDrive {}

        assertEquals(
            BackupState(
                isBackupEnabled = false,
                backupStatus = null,
            ),
            backupState.first(),
        )
    }

    @Test
    fun `running backup state`() = runTest {
        val folderId = database.myDrive {}

        addFolder(BackupFolder(0, folderId))

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                backupStatus = BackupStatus.Complete(totalBackupPhotos = 0),
            ),
            backupState.first(),
        )
    }

    @Test
    fun `uploading backup state`() = runTest {
        val folderId = database.myDrive {}

        addFolder(BackupFolder(0, folderId))

        setFiles(
            userId, listOf(
                backupFile("uri1"),
                backupFile("uri2"),
                backupFile("uri3"),
            )
        )
        markAsCompleted(userId, "uri1")


        assertEquals(
            BackupState(
                isBackupEnabled = true,
                backupStatus = BackupStatus.InProgress(
                    totalBackupPhotos = 3,
                    pendingBackupPhotos = 2,
                )
            ),
            backupState.first(),
        )
    }
    @Test
    fun failure() = runTest {
        val folderId = database.myDrive {}

        addFolder(BackupFolder(0, folderId))

        setFiles(
            userId, listOf(
                backupFile("uri1"),
                backupFile("uri2"),
                backupFile("uri3"),
            )
        )
        markAsFailed(userId, "uri1")

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                backupStatus = BackupStatus.InProgress(
                    totalBackupPhotos = 3,
                    pendingBackupPhotos = 2,
                )
            ),
            backupState.first(),
        )
    }

    @Test
    fun uncompleted() = runTest {
        val folderId = database.myDrive {}

        addFolder(BackupFolder(0, folderId))

        setFiles(
            userId, listOf(
                backupFile("uri1"),
                backupFile("uri2"),
                backupFile("uri3"),
            )
        )
        markAsCompleted(userId, "uri1")
        markAsCompleted(userId, "uri2")
        markAsFailed(userId, "uri3")

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                backupStatus = BackupStatus.Uncompleted(
                    totalBackupPhotos = 3,
                    failedBackupPhotos = 1,
                )
            ),
            backupState.first(),
        )
    }

    @Test
    fun `stopped backup state`() = runTest {
        val folderId = database.myDrive {}

        addFolder(BackupFolder(0, folderId))
        deleteFolders(userId)

        assertEquals(
            BackupState(
                isBackupEnabled = false,
                backupStatus = null,
            ),
            backupState.first(),
        )
    }
}


private fun backupFile(uriString: String) = BackupFile(
    bucketId = 0,
    uriString = uriString,
    mimeType = "",
    name = "",
    hash = "",
    size = 0.bytes,
    state = BackupFileState.IDLE,
    date = TimestampS(0),
)
