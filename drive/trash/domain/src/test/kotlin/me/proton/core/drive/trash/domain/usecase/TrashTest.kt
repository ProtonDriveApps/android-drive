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
package me.proton.core.drive.trash.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.firstSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.drivelink.domain.usecase.GetDriveLink
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linktrash.domain.entity.TrashState
import me.proton.core.drive.share.domain.usecase.GetShares
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.deleteMultiple
import me.proton.core.drive.test.api.restoreMultiple
import me.proton.core.drive.test.api.trash
import me.proton.core.drive.test.api.trashMultiple
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
class TrashTest {

    @get:Rule
    var driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var fileId1: FileId
    private lateinit var fileId2: FileId
    private lateinit var fileId3: FileId

    @Inject
    lateinit var deleteFromTrash: DeleteFromTrash

    @Inject
    lateinit var emptyTrash: EmptyTrash

    @Inject
    lateinit var sendToTrash: SendToTrash

    @Inject
    lateinit var restoreFromTrash: RestoreFromTrash

    @Inject
    lateinit var getDriveLink: GetDriveLink

    @Inject
    lateinit var getShares: GetShares

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles {
            file("file-id-1")
            file("file-id-2")
            folder("folder-id-1") {
                file("file-id-3")
            }
        }
        fileId1 = FileId(folderId.shareId, "file-id-1")
        fileId2 = FileId(folderId.shareId, "file-id-2")
        fileId3 = FileId(folderId.shareId, "file-id-3")
    }

    @Test
    fun deleteFromTrash() = runTest {
        driveRule.server.deleteMultiple()

        deleteFromTrash(userId, fileId1)

        assertNull(getFile(fileId1).getOrNull())
    }

    @Test
    fun `emptyTrash with volumeId`() = runTest {
        driveRule.server.run {
            trash()
            trashMultiple()
        }
        sendToTrash(userId, listOf(getFile(fileId1).getOrThrow()))

        emptyTrash(userId, volumeId)

        assertTrashStateOf(fileId1, TrashState.DELETED)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `emptyTrash without volumeId`() = runTest {
        driveRule.server.run {
            trash()
            trashMultiple()
        }
        sendToTrash(userId, listOf(getFile(fileId1).getOrThrow()))

        emptyTrash(userId)

        assertTrashStateOf(fileId1, TrashState.DELETED)
    }

    @Test
    fun `sendToTrash same folder`() = runTest {
        driveRule.server.trashMultiple()

        sendToTrash(userId, listOf(getFile(fileId1).getOrThrow(), getFile(fileId2).getOrThrow()))

        assertTrashStateOf(fileId1, TrashState.TRASHED)
        assertTrashStateOf(fileId2, TrashState.TRASHED)
    }

    @Test
    fun `sendToTrash two folders`() = runTest {
        driveRule.server.trashMultiple()

        sendToTrash(userId, listOf(getFile(fileId1).getOrThrow(), getFile(fileId3).getOrThrow()))

        assertTrashStateOf(fileId1, TrashState.TRASHED)
        assertTrashStateOf(fileId3, TrashState.TRASHED)
    }

    @Test
    fun restoreFromTrash() = runTest {
        driveRule.server.restoreMultiple()

        restoreFromTrash(userId, fileId1)

        assertTrashStateOf(fileId1, null)
    }

    private suspend fun assertTrashStateOf(fileId: FileId, trashState: TrashState?) {
        assertEquals(
            trashState,
            getFile(fileId).getOrThrow().trashState
        )
    }

    private suspend fun getFile(fileId: FileId) =
        getDriveLink(fileId, refresh = flowOf(false)).firstSuccessOrError().toResult()
}
