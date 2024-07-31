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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.entity.BackupPermissions
import me.proton.core.drive.backup.domain.entity.BackupState
import me.proton.core.drive.backup.domain.entity.BackupStatus
import me.proton.core.drive.backup.domain.entity.BucketEntry
import me.proton.core.drive.backup.domain.manager.BackupPermissionsManager
import me.proton.core.drive.backup.domain.repository.TestBucketRepository
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.usecase.TestIsBackgroundRestricted
import me.proton.core.drive.user.domain.entity.UserMessage
import me.proton.core.drive.user.domain.usecase.CancelUserMessage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetBackupStateTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var deleteFolders: DeleteFolders

    @Inject
    lateinit var setFiles: SetFiles

    @Inject
    lateinit var markAsCompleted: MarkAsCompleted

    @Inject
    lateinit var markAsFailed: MarkAsFailed

    @Inject
    lateinit var cancelUserMessage: CancelUserMessage

    @Inject
    lateinit var permissionsManager: BackupPermissionsManager

    @Inject
    lateinit var getBackupState: GetBackupState

    @Inject
    lateinit var isBackgroundRestricted: TestIsBackgroundRestricted

    @Inject
    lateinit var bucketRepository: TestBucketRepository

    private lateinit var backupState: Flow<BackupState>

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles {}
        permissionsManager.onPermissionChanged(BackupPermissions.Granted)

        backupState = getBackupState(folderId)
        bucketRepository.bucketEntries = listOf(BucketEntry(0, "Camera"))
    }

    @Test
    fun `blank backup state`() = runTest {
        bucketRepository.bucketEntries = emptyList()

        assertEquals(
            BackupState(
                isBackupEnabled = false,
                hasDefaultFolder = false,
                backupStatus = null,
            ),
            backupState.first(),
        )
    }

    @Test
    fun `blank backup state with folder`() = runTest {
        driveRule.db.myFiles {}

        assertEquals(
            BackupState(
                isBackupEnabled = false,
                hasDefaultFolder = true,
                backupStatus = null,
            ),
            backupState.first(),
        )
    }

    @Test
    fun `running backup state`() = runTest {
        addFolder(BackupFolder(0, folderId)).getOrThrow()

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                hasDefaultFolder = true,
                backupStatus = BackupStatus.Complete(totalBackupPhotos = 0),
            ),
            backupState.first(),
        )
    }

    @Test
    fun `uploading backup state`() = runTest {
        addFolder(BackupFolder(0, folderId)).getOrThrow()

        setFiles(
            listOf(
                backupFile("uri1"),
                backupFile("uri2"),
                backupFile("uri3"),
            )
        ).getOrThrow()
        markAsCompleted(folderId, "uri1").getOrThrow()


        assertEquals(
            BackupState(
                isBackupEnabled = true,
                hasDefaultFolder = true,
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
        addFolder(BackupFolder(0, folderId)).getOrThrow()

        setFiles(
            listOf(
                backupFile("uri1"),
                backupFile("uri2"),
                backupFile("uri3"),
            )
        ).getOrThrow()
        markAsFailed(folderId, "uri1").getOrThrow()

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                hasDefaultFolder = true,
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
        addFolder(BackupFolder(0, folderId)).getOrThrow()

        setFiles(
            listOf(
                backupFile("uri1"),
                backupFile("uri2"),
                backupFile("uri3"),
            )
        ).getOrThrow()
        markAsCompleted(folderId, "uri1").getOrThrow()
        markAsCompleted(folderId, "uri2").getOrThrow()
        markAsFailed(folderId, "uri3").getOrThrow()

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                hasDefaultFolder = true,
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

        addFolder(BackupFolder(0, folderId)).getOrThrow()
        deleteFolders(folderId).getOrThrow()

        assertEquals(
            BackupState(
                isBackupEnabled = false,
                hasDefaultFolder = true,
                backupStatus = null,
            ),
            backupState.first(),
        )
    }

    @Test
    fun `background restricted`() = runTest {
        addFolder(BackupFolder(0, folderId)).getOrThrow()

        isBackgroundRestricted.mutableStateFlow.value = true

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                hasDefaultFolder = true,
                backupStatus = BackupStatus.Failed(
                    totalBackupPhotos = 0,
                    pendingBackupPhotos = 0,
                    errors = listOf(BackupError.BackgroundRestrictions())
                ),
            ),
            backupState.first(),
        )
    }

    @Test
    fun `background restricted ignored`() = runTest {
        addFolder(BackupFolder(0, folderId)).getOrThrow()

        isBackgroundRestricted.mutableStateFlow.value = true
        cancelUserMessage(folderId.userId, UserMessage.BACKUP_BATTERY_SETTINGS).getOrThrow()

        assertEquals(
            BackupState(
                isBackupEnabled = true,
                hasDefaultFolder = true,
                backupStatus = BackupStatus.Complete(totalBackupPhotos = 0)
            ),
            backupState.first(),
        )
    }

    private fun backupFile(uriString: String) = BackupFile(
        bucketId = 0,
        folderId = folderId,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = "",
        size = 0.bytes,
        state = BackupFileState.READY,
        date = TimestampS(0),
    )
}
