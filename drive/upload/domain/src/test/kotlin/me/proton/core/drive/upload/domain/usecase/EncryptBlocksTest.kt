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

package me.proton.core.drive.upload.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.size
import me.proton.core.drive.base.domain.usecase.GetPermanentFolder
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
import me.proton.core.drive.linkupload.domain.usecase.GetUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.InsertRawUploadBlocks
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.getPublicAddressKeysAll
import me.proton.core.test.kotlin.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class EncryptBlocksTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @get: Rule
    val temporaryFolder = TemporaryFolder()

    @Inject lateinit var encryptBlocks: EncryptBlocks
    @Inject lateinit var insertRawUploadBlocks: InsertRawUploadBlocks
    @Inject lateinit var getSplitRawBlocks: GetSplitRawBlocks
    @Inject lateinit var getUploadBlocks: GetUploadBlocks
    @Inject lateinit var getPermanentFolder: GetPermanentFolder

    private lateinit var parentFolderId: FolderId
    private lateinit var blockFolder: File

    @Before
    fun before() = runTest {
        parentFolderId = driveRule.db.myFiles { }
        driveRule.server.run {
            getPublicAddressKeysAll()
        }
        blockFolder = getPermanentFolder(
            userId = userId,
            volumeId = volumeId.id,
            revisionId = "draft-revision-id"
        ).also { it.mkdirs() }
    }

    @Test
    fun `happy path`() = runTest {
        // Given
        val rawFiles = createFiles(blockFolder, 10)

        val uploadFileLink = uploadFileLink(rawFiles.sumOf { file -> file.length() }.bytes)
        insertLinkUploadEntity(uploadFileLink)
        insertRawUploadBlocks(
            rawFiles.mapIndexed { index, file ->
                RawBlock(uploadFileLink.id, index.toLong(), file.name)
            }.toSet()
        ).getOrThrow()

        // When
        encryptBlocks(
            uploadFileLink = uploadFileLink,
            uriString = "",
            isCancelled = { false },
            outputFileSuffix = "_1",
        ).getOrThrow()

        // Then
        val uploadBlocks = getUploadBlocks(uploadFileLink).getOrThrow()
        uploadBlocks.forEach { uploadBlock ->
            assertTrue(uploadBlock.file.exists()) { "Encrypted file ${uploadBlock.file.name} does not exist" }
            assertTrue(uploadBlock.file.size == 4.MiB) { "Encrypted file size is ${uploadBlock.file.size}" }
        }
        assert(uploadBlocks.size == rawFiles.size) {
            "Actual number of upload blocks is ${uploadBlocks.size}, raw files size is ${rawFiles.size}"
        }
        val rawBlocks = getSplitRawBlocks(uploadFileLink.id, blockFolder).getOrThrow()
        assertTrue(rawBlocks.isEmpty()) { "Actual number of raw blocks ${rawBlocks.size}" }
        assert(blockFolder.children.size > rawBlocks.size) {
            "Actual block folder children size is ${blockFolder.children.size}"
        }
    }

    @Test
    fun `cancelling encryptBlocks while running should keep encrypted blocks`() = runTest {
        // Given
        val rawFiles = createFiles(blockFolder, 10)
        val uploadFileLink = uploadFileLink(rawFiles.sumOf { file -> file.length() }.bytes)
        insertLinkUploadEntity(uploadFileLink)
        insertRawUploadBlocks(
            rawFiles.mapIndexed { index, file ->
                RawBlock(uploadFileLink.id, index.toLong(), file.name)
            }.toSet()
        ).getOrThrow()

        // When
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.IO + job)
        coroutineScope.launch {
            encryptBlocks(
                uploadFileLink = uploadFileLink,
                uriString = "",
                isCancelled = {
                    if (blockFolder.encryptedBlockCount() >= 2) {
                        job.cancel()
                    }
                    job.isCancelled
                },
            ).getOrThrow()
        }
        job.join()

        // Then
        val uploadBlocks = getUploadBlocks(uploadFileLink).getOrThrow()
        uploadBlocks.forEach { uploadBlock ->
            assertTrue(uploadBlock.file.exists()) { "Encrypted file ${uploadBlock.file.name} does not exist" }
        }
        assert(uploadBlocks.size < rawFiles.size) {
            "Actual number of upload blocks is ${uploadBlocks.size}, number of raw files is ${rawFiles.size}"
        }
        val rawBlocks = getSplitRawBlocks(uploadFileLink.id, blockFolder).getOrThrow()
        assert(rawBlocks.size == rawFiles.size - uploadBlocks.size) {
            "Actual number of raw blocks is ${rawBlocks.size}, number of upload blocks is ${uploadBlocks.size}"
        }
    }

    @Test
    fun `continue cancelled encryptBlocks should encrypt all blocks`() = runTest {
        // Given
        val rawFiles = createFiles(blockFolder, 7)
        val uploadFileLink = uploadFileLink(rawFiles.sumOf { file -> file.length() }.bytes)
        insertLinkUploadEntity(uploadFileLink)
        insertRawUploadBlocks(
            rawFiles.mapIndexed { index, file ->
                RawBlock(uploadFileLink.id, index.toLong(), file.name)
            }.toSet()
        ).getOrThrow()
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.IO + job)
        coroutineScope.launch {
            encryptBlocks(
                uploadFileLink = uploadFileLink,
                uriString = "",
                isCancelled = {
                    if (blockFolder.encryptedBlockCount() >= 3) {
                        job.cancel()
                    }
                    job.isCancelled
                },
            ).getOrThrow()
        }
        job.join()

        // When
        encryptBlocks(
            uploadFileLink = uploadFileLink,
            uriString = "",
            isCancelled = { false },
            outputFileSuffix = "_1",
        ).getOrThrow()

        // Then
        val uploadBlocks = getUploadBlocks(uploadFileLink).getOrThrow()
        uploadBlocks.forEach { uploadBlock ->
            assertTrue(uploadBlock.file.exists()) { "Encrypted file ${uploadBlock.file.name} does not exist" }
            assertTrue(uploadBlock.file.size == 4.MiB) { "Encrypted file size is ${uploadBlock.file.size}" }
        }
        assert(uploadBlocks.size == rawFiles.size) {
            "Actual number of upload blocks is ${uploadBlocks.size}, raw files size is ${rawFiles.size}"
        }
        val rawBlocks = getSplitRawBlocks(uploadFileLink.id, blockFolder).getOrThrow()
        assertTrue(rawBlocks.isEmpty()) { "Actual number of raw blocks ${rawBlocks.size}" }
        assert(blockFolder.children.size > rawBlocks.size) {
            "Actual block folder children size is ${blockFolder.children.size}"
        }
    }

    private fun createFiles(parentFolder: File, count: Int): List<File> =
        (1..count).map {
            createFile(parentFolder, 4.MiB)
        }

    private suspend fun insertLinkUploadEntity(uploadFileLink: UploadFileLink) {
        driveRule.db.linkUploadDao.insertOrUpdate(
            uploadFileLink.toLinkUploadEntity()
        )
    }

    private fun File.encryptedBlockCount() = listFiles().orEmpty()
        .filterNot { it.name.endsWith(".txt") }
        .filterNot { it.length() == 0L }
        .size

    private fun uploadFileLink(size: Bytes) = UploadFileLink(
        id = 1L,
        userId = userId,
        volumeId = volumeId,
        shareId = mainShareId,
        parentLinkId = parentFolderId,
        name = "name",
        mimeType = "text/plain",
        size = size,
        state = UploadState.IDLE,
        networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
        priority = UploadFileLink.USER_PRIORITY,
        draftRevisionId = "draft-revision-id",
        nodeKey = "node-key-upload",
        nodePassphrase = "u".repeat(32),
        contentKeyPacket = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    )
}
