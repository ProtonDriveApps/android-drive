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
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.repository.BackupDuplicateRepository
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.fileDeleteFolderChildren
import me.proton.core.network.domain.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CleanRevisionsTest {
    @get:Rule
    val driveRule = DriveRule(this)

    private lateinit var folderId: FolderId

    @Inject
    lateinit var cleanRevisions: CleanRevisions

    @Inject
    lateinit var backupDuplicateRepository: BackupDuplicateRepository

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
    }

    @Test
    fun empty() = runTest {
        cleanRevisions(folderId).getOrThrow()

        assertEquals(
            emptyList<BackupDuplicate>(),
            backupDuplicateRepository.getAll(folderId, 0, 100)
        )
    }

    @Test
    fun activeRevision() = runTest {
        val duplicates = listOf(
            backupDuplicate(state = Link.State.ACTIVE)
        )
        backupDuplicateRepository.insertDuplicates(
            duplicates
        )

        cleanRevisions(folderId).getOrThrow()

        assertEquals(
            duplicates,
            backupDuplicateRepository.getAll(folderId, 0, 100)
        )
    }

    @Test
    fun draftRevision() = runTest {
        driveRule.server.fileDeleteFolderChildren()
        backupDuplicateRepository.insertDuplicates(
            listOf(
                backupDuplicate(state = Link.State.DRAFT)
            )
        )

        cleanRevisions(folderId).getOrThrow()

        assertEquals(
            emptyList<BackupDuplicate>(),
            backupDuplicateRepository.getAll(folderId, 0, 100)
        )
    }

    @Test
    fun error() = runTest {
        driveRule.server.fileDeleteFolderChildren { errorResponse() }
        val duplicates = listOf(
            backupDuplicate(state = Link.State.DRAFT)
        )
        backupDuplicateRepository.insertDuplicates(
            duplicates
        )

        val result = cleanRevisions(folderId)
        assertThrows(ApiException::class.java) { result.getOrThrow() }

        assertEquals(
            duplicates,
            backupDuplicateRepository.getAll(folderId, 0, 100)
        )
    }

    private fun backupDuplicate(state: Link.State, id: Long = 1) = BackupDuplicate(
        id = id,
        parentId = folderId,
        hash = "hash",
        contentHash = null,
        linkId = FileId(folderId.shareId, "link-id"),
        linkState = state,
        revisionId = "revision-Id",
        clientUid = null,
    )
}
