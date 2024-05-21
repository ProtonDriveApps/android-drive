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
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.drivePhotosUploadDisabled
import me.proton.core.drive.feature.flag.domain.usecase.GetFeatureFlag
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.coreFeatures
import me.proton.core.drive.test.api.featureFrontend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class RetryBackupTest {


    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var backupManager: StubbedBackupManager

    @Inject
    lateinit var getErrors: GetErrors

    @Inject
    lateinit var getFeatureFlag: GetFeatureFlag

    @Inject
    lateinit var addBackupError: AddBackupError

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var setFiles: SetFiles

    @Inject
    lateinit var fileRepository: BackupFileRepository

    @Inject
    lateinit var retryBackup: RetryBackup

    @Before
    fun setup() = runTest {
        folderId = driveRule.db.myFiles { }
        driveRule.server.coreFeatures()
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
    }

    @Test
    fun `Given PhotosUploadNotAllowed error when retry should refresh feature flag `() = runTest {
        driveRule.server.featureFrontend(FeatureFlagId.DRIVE_PHOTOS_UPLOAD_DISABLED)
        addBackupError(folderId, BackupError.PhotosUploadNotAllowed()).getOrThrow()

        retryBackup(folderId).getOrThrow()

        assertTrue(backupManager.started)
        assertEquals(
            emptyList<BackupError>(),
            getErrors(folderId).first()
        )
        assertEquals(
            FeatureFlag(drivePhotosUploadDisabled(userId), ENABLED),
            getFeatureFlag(drivePhotosUploadDisabled(userId)) { false }
        )
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
