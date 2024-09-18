/*
 * Copyright (c) 2024 Proton AG.
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

package me.proton.core.drive.linkupload.domain.usecase

import android.database.sqlite.SQLiteConstraintException
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.KiB
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.data.extension.toLinkUploadEntity
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.RawBlock
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.test.DriveRule
import me.proton.core.test.kotlin.assertEquals
import me.proton.core.test.kotlin.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class RawUploadBlocksTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject lateinit var linkUploadRepository: LinkUploadRepository
    @Inject lateinit var getRawUploadBlocks: GetRawUploadBlocks
    @Inject lateinit var removeAllRawUploadBlocks: RemoveAllRawUploadBlocks
    @Inject lateinit var insertRawUploadBlocks: InsertRawUploadBlocks
    @Inject lateinit var removeRawUploadBlock: RemoveRawUploadBlock

    private lateinit var parentFolderId: FolderId

    @Before
    fun before() = runTest {
        parentFolderId = driveRule.db.myFiles {}
        driveRule.db.linkUploadDao.insertOrUpdate(
            uploadFileLink.toLinkUploadEntity()
        )
    }

    @Test
    fun `when there is no raw blocks getRawUploadBlocks returns empty list`() = runTest {
        // When
        val rawBlocks = getRawUploadBlocks(uploadFileLink.id).getOrThrow()

        // Then
        assertTrue(rawBlocks.isEmpty()) { "Actual rawBlocks size: ${rawBlocks.size}" }
    }

    @Test
    fun `when getting raw blocks with non-existent upload file link id getRawUploadBlocks returns empty list`() = runTest {
        // When
        val rawBlocks = getRawUploadBlocks(uploadFileLink.id + 123).getOrThrow()

        // Then
        assertTrue(rawBlocks.isEmpty()) { "Actual rawBlocks size: ${rawBlocks.size}" }
    }

    @Test
    fun `when raw block is inserted via insertRawUploadBlocks, block list size is one and it contains same raw block`() = runTest {
        // Given
        val rawBlock = RawBlock(
            uploadFileLinkId = uploadFileLink.id,
            index = 1L,
            name = "block",
        )

        // When
        insertRawUploadBlocks(setOf(rawBlock)).getOrThrow()
        val rawBlocks = getRawUploadBlocks(uploadFileLink.id).getOrThrow()

        // Then
        assertTrue(rawBlocks.size == 1) {
            "Actual raw blocks size: ${rawBlocks.size}"
        }
        assertEquals(rawBlock, rawBlocks.first()) { "Raw blocks are not equal" }
    }

    @Test(expected = SQLiteConstraintException::class)
    fun `insert raw block with non-existent upload file link id throws exception`() = runTest {
        // Given
        val rawBlock = RawBlock(
            uploadFileLinkId = 123L, // Invalid upload file link id
            index = 1L,
            name = "block",
        )

        // When
        insertRawUploadBlocks(setOf(rawBlock)).getOrThrow()
    }

    @Test
    fun `removed block is no longer in list of raw blocks`() = runTest {
        // Given
        val blocks = setOf(
            RawBlock(
                uploadFileLinkId = uploadFileLink.id,
                index = 1L,
                name = "first block",
            ),
            RawBlock(
                uploadFileLinkId = uploadFileLink.id,
                index = 2L,
                name = "second block",
            ),
        )
        insertRawUploadBlocks(blocks).getOrThrow()

        // When
        removeRawUploadBlock(uploadFileLink.id, 1L).getOrThrow()
        val rawBlocks = getRawUploadBlocks(uploadFileLink.id).getOrThrow()

        // Then
        assertTrue(rawBlocks.size == 1) { "Actual rawBlocks size: ${rawBlocks.size}" }
        assertEquals(blocks.last(), rawBlocks.first()) { "Raw blocks are not equal" }
    }

    @Test
    fun `after remove all list of raw blocks is empty`() = runTest {
        // Given
        val blocks = setOf(
            RawBlock(
                uploadFileLinkId = uploadFileLink.id,
                index = 1L,
                name = "first block",
            ),
            RawBlock(
                uploadFileLinkId = uploadFileLink.id,
                index = 2L,
                name = "second block",
            ),
        )
        insertRawUploadBlocks(blocks).getOrThrow()

        // When
        removeAllRawUploadBlocks(uploadFileLink.id).getOrThrow()
        val rawBlocks = getRawUploadBlocks(uploadFileLink.id).getOrThrow()

        // Then
        assertTrue(rawBlocks.isEmpty()) { "Actual rawBlocks size: ${rawBlocks.size}" }
    }

    private val uploadFileLink: UploadFileLink get() = UploadFileLink(
        id = 1L,
        userId = userId,
        volumeId = volumeId,
        shareId = mainShareId,
        parentLinkId = parentFolderId,
        name = "name",
        mimeType = "text/plain",
        size = 20.KiB,
        state = UploadState.IDLE,
        networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
        priority = UploadFileLink.USER_PRIORITY,
    )
}
