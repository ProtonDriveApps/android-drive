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

package me.proton.core.drive.backup.data.repository

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
class BackupErrorRepositoryImplTest {
    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @Inject
    lateinit var repository: BackupErrorRepositoryImpl

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
            repository.getAll(folderId, 0, 100).first(),
        )
    }

    @Test
    fun getAll() = runTest {
        repository.insertError(folderId, retryable)
        repository.insertError(folderId, nonRetryable)

        assertEquals(
            listOf(retryable, nonRetryable),
            repository.getAll(folderId, 0, 100).first(),
        )
    }

    @Test
    fun deleteAll() = runTest {
        repository.insertError(folderId, retryable)
        repository.insertError(folderId, nonRetryable)

        repository.deleteAll(folderId)

        assertEquals(
            emptyList<BackupError>(),
            repository.getAll(folderId, 0, 100).first(),
        )
    }

    @Test
    fun deleteAllByType() = runTest {
        repository.insertError(folderId, retryable)
        repository.insertError(folderId, nonRetryable)

        repository.deleteAllByType(folderId, nonRetryable.type)

        assertEquals(
            listOf(retryable),
            repository.getAll(folderId, 0, 100).first(),
        )
    }

    @Test
    fun deleteAllRetryable() = runTest {
        repository.insertError(folderId, retryable)
        repository.insertError(folderId, nonRetryable)

        repository.deleteAllRetryable(folderId)

        assertEquals(
            listOf(nonRetryable),
            repository.getAll(folderId, 0, 100).first(),
        )
    }
}
