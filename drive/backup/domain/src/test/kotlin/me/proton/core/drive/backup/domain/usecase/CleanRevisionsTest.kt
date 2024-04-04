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

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.BackupDuplicateRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.folder.domain.repository.FolderRepository
import me.proton.core.drive.folder.domain.usecase.DeleteFolderChildren
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CleanRevisionsTest {
    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var folderId: FolderId
    private lateinit var cleanRevisions: CleanRevisions
    private lateinit var backupDuplicateRepository: BackupDuplicateRepositoryImpl
    private val folderRepository: FolderRepository = mockk()

    @Before
    fun setUp() = runTest {
        folderId = database.myFiles { }
        backupDuplicateRepository = BackupDuplicateRepositoryImpl(database.db)
        cleanRevisions = CleanRevisions(
            repository = backupDuplicateRepository,
            configurationProvider = object : ConfigurationProvider {
                override val host: String = ""
                override val baseUrl: String = ""
                override val appVersionHeader: String = ""
            },
            deleteFolderChildren = DeleteFolderChildren(folderRepository)
        )
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
        coEvery { folderRepository.deleteFolderChildren(any(), any()) } returns Result.success(Unit)
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
        val failure = Result.failure<Unit>(Throwable())
        coEvery { folderRepository.deleteFolderChildren(any(), any()) } returns failure
        val duplicates = listOf(
            backupDuplicate(state = Link.State.DRAFT)
        )
        backupDuplicateRepository.insertDuplicates(
            duplicates
        )

        assertThrows(Throwable::class.java){
            runBlocking { cleanRevisions(folderId).getOrThrow() }
        }

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
