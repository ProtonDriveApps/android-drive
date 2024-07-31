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
package me.proton.core.drive.trash.domain.notification

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.api.ProtonApiCode
import me.proton.core.drive.base.data.api.response.Response
import me.proton.core.drive.base.domain.extension.firstSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.link.data.api.response.LinkResponse
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.clear
import me.proton.core.drive.test.api.deleteMultiple
import me.proton.core.drive.test.api.restoreMultiple
import me.proton.core.drive.test.api.trashMultiple
import me.proton.core.drive.trash.domain.usecase.DeleteFromTrash
import me.proton.core.drive.trash.domain.usecase.EmptyTrash
import me.proton.core.drive.trash.domain.usecase.RestoreFromTrash
import me.proton.core.drive.trash.domain.usecase.SendToTrash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
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
    var driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var fileId: FileId

    @Inject
    lateinit var getDriveLink: GetDriveLink

    @Inject
    lateinit var actionProvider: TrashExtraActionProvider

    @Inject
    lateinit var deleteFromTrash: DeleteFromTrash

    @Inject
    lateinit var emptyTrash: EmptyTrash

    @Inject
    lateinit var sendToTrash: SendToTrash

    @Inject
    lateinit var restoreFromTrash: RestoreFromTrash

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles {
            file("file-id-1")
        }
        fileId = FileId(folderId.shareId, "file-id-1")
    }

    @Test
    fun `no exception during delete`() = runTest {
        assertNull(
            actionProvider.provideAction(
                DeleteFilesExtra(
                    userId = userId,
                    shareId = folderId.shareId,
                    links = listOf(fileId),
                )
            )
        )
    }

    @Test
    fun `exception during delete`() = runTest {
        driveRule.server.deleteMultiple()

        actionProvider.provideAction(
            DeleteFilesExtra(
                userId = userId,
                shareId = folderId.shareId,
                links = listOf(fileId),
                exception = RuntimeException(),
            )
        )!!.invoke()

        assertNull(getFile(fileId).getOrNull())
    }

    @Test
    fun `no exception during empty trash`() = runTest {
        assertNull(
            actionProvider.provideAction(
                EmptyTrashExtra(
                    userId = userId,
                    shareId = folderId.shareId,
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
                    shareId = folderId.shareId,
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
                    shareId = folderId.shareId,
                    links = listOf(fileId),
                )
            )
        )
    }

    @Test
    fun `exception during restore`() = runTest {
        driveRule.server.run {
            restoreMultiple { linkId ->
                LinkResponse(linkId, response = Response(ProtonApiCode.INVALID_VALUE.toLong()))
            }
        }

        restoreFromTrash(userId, fileId)

        assertTrashStateOf(fileId, TrashState.TRASHED)

        driveRule.server.run {
            clear()
            restoreMultiple()
        }

        actionProvider.provideAction(
            RestoreFilesExtra(
                userId = userId,
                shareId = folderId.shareId,
                links = listOf(fileId),
                exception = RuntimeException(),
            )
        )!!.invoke()

        assertTrashStateOf(fileId, null)
    }
    @Test
    fun `no exception during trash`() = runTest {
        driveRule.server.run {
            trashMultiple()
            restoreMultiple()
        }
        sendToTrash(userId, getFile(fileId).getOrThrow())

        assertTrashStateOf(fileId, TrashState.TRASHED)

        actionProvider.provideAction(
            TrashFilesExtra(
                userId = userId,
                folderId = folderId,
                links = listOf(fileId),
            )
        )!!.invoke()

        assertTrashStateOf(fileId, null)
    }

    @Test
    fun `exception during trash`() = runTest {
        driveRule.server.trashMultiple()

        actionProvider.provideAction(
            TrashFilesExtra(
                userId = userId,
                folderId = folderId,
                links = listOf(fileId),
                exception = RuntimeException(),
            )
        )!!.invoke()

        assertTrashStateOf(fileId, TrashState.TRASHED)
    }

    private suspend fun assertTrashStateOf(fileId: FileId, trashState: TrashState?) {
        assertEquals(
            trashState,
            getFile(fileId).getOrThrow().trashState
        )
    }

    private suspend fun getFile(fileId: FileId) =
        getDriveLink(fileId).firstSuccessOrError().toResult()
}
