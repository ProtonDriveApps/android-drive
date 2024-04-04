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
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.NoNetworkConfigurationProvider
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FolderId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ErrorTest {
    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var getErrors: GetErrors
    private lateinit var addBackupError: AddBackupError
    private lateinit var deleteAllBackupError: DeleteAllBackupError
    private lateinit var deleteAllRetryableBackupError: DeleteAllRetryableBackupError

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
        folderId = database.myFiles { }
        val repository = BackupErrorRepositoryImpl(database.db)
        addBackupError = AddBackupError(repository)
        deleteAllBackupError = DeleteAllBackupError(repository)
        deleteAllRetryableBackupError = DeleteAllRetryableBackupError(repository)
        getErrors = GetErrors(repository, NoNetworkConfigurationProvider.instance)
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
