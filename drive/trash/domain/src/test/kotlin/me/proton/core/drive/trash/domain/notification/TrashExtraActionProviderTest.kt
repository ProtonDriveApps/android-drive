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
package me.proton.core.drive.trash.domain.notification

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linktrash.data.test.repository.state
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.linktrash.domain.repository.LinkTrashRepository
import me.proton.core.drive.share.domain.entity.ShareId
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TrashExtraActionProviderTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: LinkTrashRepository

    @Inject
    lateinit var actionProvider: TrashExtraActionProvider

    private val userId = UserId("user-id")
    private val shareId = ShareId(userId, "share-id")
    private val folderId = FolderId(shareId, "folder-id")
    private val fileId = FileId(shareId, "file-id")

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `no exception during delete`() = runTest {
        assertNull(
            actionProvider.provideAction(
                DeleteFilesExtra(
                    userId = userId,
                    shareId = shareId,
                    links = listOf(fileId),
                )
            )
        )
    }

    @Test
    @Ignore("Breaks with getShare usage in deleteFromTrash use case")
    fun `exception during delete`() = runTest {
        actionProvider.provideAction(
            DeleteFilesExtra(
                userId = userId,
                shareId = shareId,
                links = listOf(fileId),
                exception = RuntimeException(),
            )
        )?.invoke()

        assertEquals(TrashState.DELETING, repository.state[listOf(fileId)])
    }

    @Test
    fun `no exception during empty trash`() = runTest {
        assertNull(
            actionProvider.provideAction(
                EmptyTrashExtra(
                    userId = userId,
                    shareId = shareId,
                )
            )
        )
    }

    @Test
    fun `exception during empty trash`() = runTest {
        assertNull(
            actionProvider.provideAction(
                EmptyTrashExtra(
                    userId = userId,
                    shareId = shareId,
                    exception = RuntimeException(),
                    
                )
            )
        )
    }

    @Test
    fun `no exception during restore`() = runTest {
        assertNull(
            actionProvider.provideAction(
                RestoreFilesExtra(
                    userId = userId,
                    shareId = shareId,
                    links = listOf(fileId),
                )
            )
        )
    }

    @Test
    @Ignore("Breaks with getShare usage in restoreFromTrash use case")
    fun `exception during restore`() = runTest {
        actionProvider.provideAction(
            RestoreFilesExtra(
                userId = userId,
                shareId = shareId,
                links = listOf(fileId),
                exception = RuntimeException(),
            )
        )?.invoke()

        assertEquals(TrashState.RESTORING, repository.state[listOf(fileId)])
    }
    @Test
    @Ignore("Breaks with getShare usage in restoreFromTrash use case")
    fun `no exception during trash`() = runTest {
        actionProvider.provideAction(
            TrashFilesExtra(
                userId = userId,
                folderId = folderId,
                links = listOf(fileId),
            )
        )?.invoke()

        assertEquals(TrashState.RESTORING, repository.state[listOf(fileId)])
    }

    @Test
    @Ignore("Breaks with getShare usage in sendToTrash use case")
    fun `exception during trash`() = runTest {
        actionProvider.provideAction(
            TrashFilesExtra(
                userId = userId,
                folderId = folderId,
                links = listOf(fileId),
                exception = RuntimeException(),
            )
        )?.invoke()

        assertEquals(TrashState.TRASHING, repository.state[listOf(fileId)])
    }
}