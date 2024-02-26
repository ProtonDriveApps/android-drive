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
package me.proton.core.drive.linktrash.data.test.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StubbedLinkTrashRepositoryTest {

    private val repository = StubbedLinkTrashRepository()


    private val userId = UserId("user-id")
    private val volumeId = VolumeId("volume-id")
    private val shareId = ShareId(userId, "share-id")
    private val folderId = FolderId(shareId, "folder-id")
    private val otherShareId = ShareId(userId, "other-share-id")
    private val otherFolderId = FolderId(otherShareId, "other-folder-id")

    @Test
    fun insertOrUpdateTrashState() = runTest {
        repository.insertOrUpdateTrashState(volumeId, listOf(folderId), TrashState.TRASHING)

        assertEquals(TrashState.TRASHING, repository.state[listOf(folderId)])
    }

    @Test
    fun removeTrashState() = runTest {
        repository.insertOrUpdateTrashState(volumeId, listOf(folderId), TrashState.TRASHING)
        repository.insertOrUpdateTrashState(volumeId, listOf(otherFolderId), TrashState.TRASHING)

        repository.removeTrashState(listOf(folderId))

        assertEquals(mapOf(listOf(otherFolderId) to TrashState.TRASHING), repository.state)
    }

    @Test
    fun hasTrashContent() = runTest {
        val hasTrashContent = repository.hasTrashContent(userId, volumeId)
        assertFalse(hasTrashContent.first())

        repository.insertOrUpdateTrashState(volumeId, listOf(folderId), TrashState.TRASHING)

        assertTrue(hasTrashContent.first())
    }

    @Test
    fun hasWorkWithId() = runTest {
        assertFalse(repository.hasWorkWithId("work-id-share-id"))

        repository.insertWork(listOf(folderId))

        assertTrue(repository.hasWorkWithId("work-id-share-id"))
    }

    @Test
    fun insertOrIgnoreWorkId() = runTest {
        val workId = "work-id"

        repository.insertOrIgnoreWorkId(listOf(folderId), workId)

        assertTrue(repository.hasWorkWithId(workId))
    }

    @Test
    fun insertWork() = runTest {
        repository.insertWork(listOf(folderId))

        assertTrue(repository.hasWorkWithId("work-id-share-id"))
    }

    @Test
    fun `Given folder id When getLinksAndRemoveWorkFromCache Then returns a folder`() = runTest {
        val workId = "work-id"

        repository.insertOrIgnoreWorkId(listOf(folderId), workId)

        val links = repository.getLinksAndRemoveWorkFromCache(workId)

        assertEquals(folderId, links.first().id)
        assertFalse(repository.hasWorkWithId(workId))
    }

    @Test
    fun `Given file id When getLinksAndRemoveWorkFromCache Then returns a file`() = runTest {
        val workId = "work-id"
        val fileId = FileId(shareId, "file-id")

        repository.insertOrIgnoreWorkId(listOf(fileId), workId)

        val links = repository.getLinksAndRemoveWorkFromCache(workId)

        assertEquals(fileId, links.first().id)
        assertFalse(repository.hasWorkWithId(workId))
    }

    @Test
    fun shouldInitiallyFetchTrashContent() = runTest {
        assertTrue(repository.shouldInitiallyFetchTrashContent(userId, volumeId))

        repository.markTrashContentAsFetched(userId, volumeId)

        assertFalse(repository.shouldInitiallyFetchTrashContent(userId, volumeId))
    }

    @Test
    fun `Given empty When isTrashed Then returns false`() = runTest {
        assertFalse(repository.isTrashed(folderId))
    }

    @Test
    fun `Given trashed folder When isTrashed Then returns true`() = runTest {
        repository.insertOrUpdateTrashState(volumeId, listOf(folderId), TrashState.TRASHED)

        assertTrue(repository.isTrashed(folderId))
    }

    @Test
    fun `Given trashing folder When isTrashed Then returns false`() = runTest {
        repository.insertOrUpdateTrashState(volumeId, listOf(folderId), TrashState.TRASHING)

        assertFalse(repository.isTrashed(folderId))
    }

    @Test
    fun `Given empty When isAnyTrashed Then returns false`() = runTest {
        assertFalse(repository.isAnyTrashed(setOf(folderId)))
    }

    @Test
    fun `Given trashed folder When isAnyTrashed Then returns true`() = runTest {
        repository.insertOrUpdateTrashState(volumeId, listOf(folderId), TrashState.TRASHED)

        assertTrue(repository.isAnyTrashed(setOf(folderId)))
    }

    @Test
    fun `Given trashing folder When isAnyTrashed Then returns false`() = runTest {
        repository.insertOrUpdateTrashState(volumeId, listOf(folderId), TrashState.TRASHING)

        assertFalse(repository.isAnyTrashed(setOf(folderId)))
    }
}
