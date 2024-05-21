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
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ErrorTest {
    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var getErrors: GetErrors
    @Inject
    lateinit var addBackupError: AddBackupError
    @Inject
    lateinit var deleteAllBackupError: DeleteAllBackupError
    @Inject
    lateinit var deleteAllRetryableBackupError: DeleteAllRetryableBackupError

    private val retryable = BackupError(
        type = BackupErrorType.LOCAL_STORAGE,
        retryable = true,
    )

    private val nonRetryable = BackupError(
        type = BackupErrorType.OTHER,
        retryable = false,
    )

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
    }

    @Test
    fun empty() = runTest {
        assertEquals(
            emptyList<BackupError>(),
            getErrors(folderId).first(),
        )
    }

    @Test
    fun getErrors() = runTest {
        addBackupError(folderId, retryable)
        addBackupError(folderId, nonRetryable)

        assertEquals(
            listOf(retryable, nonRetryable),
            getErrors(folderId).first(),
        )
    }

    @Test
    fun deleteAllBackupError() = runTest {
        addBackupError(folderId, retryable)
        addBackupError(folderId, nonRetryable)

        deleteAllBackupError(folderId)

        assertEquals(
            emptyList<BackupError>(),
            getErrors(folderId).first(),
        )
    }

    @Test
    fun deleteAllByType() = runTest {
        addBackupError(folderId, retryable)
        addBackupError(folderId, nonRetryable)

        deleteAllBackupError(folderId, nonRetryable.type)

        assertEquals(
            listOf(retryable),
            getErrors(folderId).first(),
        )
    }

    @Test
    fun deleteAllRetryableBackupError() = runTest {
        addBackupError(folderId, retryable)
        addBackupError(folderId, nonRetryable)

        deleteAllRetryableBackupError(folderId)

        assertEquals(
            listOf(nonRetryable),
            getErrors(folderId).first(),
        )
    }
}
