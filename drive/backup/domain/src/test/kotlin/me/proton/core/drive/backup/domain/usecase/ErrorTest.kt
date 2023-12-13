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
import me.proton.core.drive.backup.data.repository.BackupErrorRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupErrorType
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
class ErrorTest {
    @get:Rule
    val database = DriveDatabaseRule()

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
        database.myDrive { }
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
            getErrors(userId).first(),
        )
    }

    @Test
    fun getErrors() = runTest {
        addBackupError(userId, retryable)
        addBackupError(userId, nonRetryable)

        assertEquals(
            listOf(retryable, nonRetryable),
            getErrors(userId).first(),
        )
    }

    @Test
    fun deleteAllBackupError() = runTest {
        addBackupError(userId, retryable)
        addBackupError(userId, nonRetryable)

        deleteAllBackupError(userId)

        assertEquals(
            emptyList<BackupError>(),
            getErrors(userId).first(),
        )
    }

    @Test
    fun deleteAllByType() = runTest {
        addBackupError(userId, retryable)
        addBackupError(userId, nonRetryable)

        deleteAllBackupError(userId, nonRetryable.type)

        assertEquals(
            listOf(retryable),
            getErrors(userId).first(),
        )
    }

    @Test
    fun deleteAllRetryableBackupError() = runTest {
        addBackupError(userId, retryable)
        addBackupError(userId, nonRetryable)

        deleteAllRetryableBackupError(userId)

        assertEquals(
            listOf(nonRetryable),
            getErrors(userId).first(),
        )
    }
}
