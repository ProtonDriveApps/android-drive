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

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.announce.event.domain.usecase.AnnounceEvent
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.NoNetworkConfigurationProvider
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.feature.flag.domain.repository.FeatureFlagRepository
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.FolderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RetryBackupTest {


    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var backupManager: StubbedBackupManager
    private lateinit var getErrors: GetErrors
    private lateinit var getFeatureFlag: GetFeatureFlag
    private lateinit var addBackupError: AddBackupError
    private lateinit var addFolder: AddFolder
    private lateinit var setFiles: SetFiles
    private lateinit var fileRepository: BackupFileRepository
    private lateinit var resetFilesAttempts: ResetFilesAttempts
    private lateinit var retryBackup: RetryBackup

    private val featureFlagRepository: FeatureFlagRepository = mockk(relaxed = true)

    @Before
    fun setup() = runTest {
        folderId = database.myDrive { }
        val folderRepository = BackupFolderRepositoryImpl(database.db)
        fileRepository = BackupFileRepositoryImpl(database.db)
        val errorRepository = BackupErrorRepositoryImpl(database.db)

        backupManager = StubbedBackupManager(folderRepository)
        addFolder = AddFolder(folderRepository)
        setFiles = SetFiles(fileRepository)
        addBackupError = AddBackupError(errorRepository)
        getErrors = GetErrors(errorRepository, NoNetworkConfigurationProvider.instance)
        getFeatureFlag = GetFeatureFlag(
            featureFlagRepository = featureFlagRepository,
            configurationProvider = object : ConfigurationProvider {
                override val host = ""
                override val baseUrl = ""
                override val appVersionHeader = ""
            },
        )
        resetFilesAttempts = ResetFilesAttempts(fileRepository)
        retryBackup = RetryBackup(
            startBackup = StartBackup(backupManager, AnnounceEvent(emptySet())),
            getErrors = getErrors,
            getFeatureFlag = getFeatureFlag,
            deleteAllRetryableBackupError = DeleteAllRetryableBackupError(errorRepository),
            resetFilesAttempts = resetFilesAttempts,
        )
    }

    @Test
    fun retryBackup() = runTest {
        addBackupError(folderId, BackupError.Other(retryable = true)).getOrThrow()
        addBackupError(folderId, BackupError.Other(retryable = false)).getOrThrow()

        retryBackup(folderId).getOrThrow()

        assertTrue(backupManager.started)
        assertEquals(
            listOf(BackupError.Other(retryable = false)),
            getErrors(folderId).first()
        )
        coVerify(exactly = 0) { featureFlagRepository.refresh(any()) }
    }

    @Test
    fun `Given PhotosUploadNotAllowed error when retry should refresh feature flag `() = runTest {
        addBackupError(folderId, BackupError.PhotosUploadNotAllowed()).getOrThrow()

        retryBackup(folderId).getOrThrow()

        assertTrue(backupManager.started)
        assertEquals(
            emptyList<BackupError>(),
            getErrors(folderId).first()
        )
        coVerify { featureFlagRepository.refresh(userId) }

    }

    @Test
    fun `Given file error when retry should reset file attempts `() = runTest {
        addFolder(
            BackupFolder(
                bucketId = 0,
                folderId = folderId,
            )
        ).getOrThrow()
        val backupFile = BackupFile(
            bucketId = 0,
            folderId = folderId,
            uriString = "uri",
            mimeType = "",
            name = "",
            hash = "",
            size = 0.bytes,
            state = BackupFileState.FAILED,
            date = TimestampS(0),
            attempts = 5
        )
        setFiles(listOf(backupFile)).getOrThrow()

        retryBackup(folderId).getOrThrow()

        assertTrue(backupManager.started)
        assertEquals(
            listOf(backupFile.copy(attempts = 0)),
            fileRepository.getFiles(
                folderId = folderId,
                bucketId = 0,
                fromIndex = 0,
                count = 100,
            ),
        )

    }
}
