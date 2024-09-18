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

import android.net.Uri
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.extension.size
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.data.extension.toLinkUploadEntity
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.GetRawUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.InsertRawUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.RemoveAllRawUploadBlocks
import me.proton.core.drive.linkupload.domain.usecase.UpdateDigests
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.upload.domain.exception.InconsistencyException
import me.proton.core.drive.upload.domain.resolver.UriResolver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SplitUriToBlocksTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @get: Rule
    val temporaryFolder = TemporaryFolder()

    lateinit var splitUriToBlocks: SplitUriToBlocks
    @Inject lateinit var uriResolver: UriResolver
    @Inject lateinit var updateDigests: UpdateDigests
    @Inject lateinit var getSplitRawBlocks: GetSplitRawBlocks
    @Inject lateinit var getRawUploadBlocks: GetRawUploadBlocks
    @Inject lateinit var removeAllRawUploadBlocks: RemoveAllRawUploadBlocks
    @Inject lateinit var insertRawUploadBlocks: InsertRawUploadBlocks
    @Inject lateinit var configurationProvider: ConfigurationProvider
    @Inject lateinit var updateUploadState: UpdateUploadState

    private val userId = mainShareId.userId
    private lateinit var parentFolderId: FolderId
    private lateinit var blockFolder: File
    private lateinit var sourceFolder: File
    private val getBlockFolder = mockk<GetBlockFolder>()

    @Before
    fun before() = runTest {
        parentFolderId = driveRule.db.myFiles {}
        blockFolder = temporaryFolder.newFolder()
        sourceFolder = temporaryFolder.newFolder()

        coEvery { getBlockFolder(userId, any()) } returns Result.success(blockFolder)

        splitUriToBlocks = SplitUriToBlocks(
            updateUploadState = updateUploadState,
            uriResolver = uriResolver,
            getBlockFolder = getBlockFolder,
            updateDigests = updateDigests,
            getSplitRawBlocks = getSplitRawBlocks,
            removeAllRawBlocks = removeAllRawUploadBlocks,
            insertRawBlocks = insertRawUploadBlocks,
            configurationProvider = configurationProvider,
        )
    }

    @Test
    fun `cancelling splitUriToBlocks while running should keep split blocks`() = runTest {
        // Given
        val sourceFile = createFile(sourceFolder, 119.MiB)
        val uploadFileLink = uploadFileLink(sourceFile.size)
        insertLinkUploadEntity(uploadFileLink)

        // When
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.Default + job)
        coroutineScope.launch {
            splitUriToBlocks(
                uploadFileLink = uploadFileLink,
                uriString = Uri.fromFile(sourceFile).toString(),
                isCancelled = { job.isCancelled },
            ).getOrThrow()
        }
        blockFolder.waitForContent(1)
        job.cancelAndJoin()
        val rawBlocks = getRawUploadBlocks(uploadFileLink.id).getOrThrow()

        // Then
        assert(rawBlocks.size == blockFolder.children.size) {
            "Actual blocks in database: ${rawBlocks.size} and file system: ${blockFolder.children.size}"
        }
        assert(blockFolder.children.size < 30) {
            "Actual number of files in block folder ${blockFolder.children.size}"
        }
    }

    @Test
    fun `continue cancelled splitUriToBlocks should split all blocks`() = runTest {
        // Given
        val sourceFile = createFile(sourceFolder, 119.MiB)
        val uriString = Uri.fromFile(sourceFile).toString()
        val uploadFileLink = uploadFileLink(sourceFile.size)
        insertLinkUploadEntity(uploadFileLink)
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.IO + job)
        coroutineScope.launch {
            splitUriToBlocks(
                uploadFileLink = uploadFileLink,
                uriString = uriString,
                isCancelled = { job.isCancelled },
            ).getOrThrow()
        }
        blockFolder.waitForContent(8)
        job.cancelAndJoin()

        // When
        splitUriToBlocks(
            uploadFileLink = uploadFileLink,
            uriString = uriString,
            isCancelled = { false },
        ).getOrThrow()
        val rawBlocks = getRawUploadBlocks(uploadFileLink.id).getOrThrow()

        // Then
        assert(rawBlocks.size == blockFolder.children.size) {
            "Actual blocks in database: ${rawBlocks.size} and file system: ${blockFolder.children.size}"
        }
        assert(blockFolder.children.size == 30) {
            "Actual number of files in block folder ${blockFolder.children.size}"
        }
    }

    @Test(expected = InconsistencyException::class)
    fun `if database contains entry and such file does not exists on filesystem exception is thrown`() = runTest {
        // Given
        val sourceFile = createFile(sourceFolder, 119.MiB)
        val uriString = Uri.fromFile(sourceFile).toString()
        val uploadFileLink = uploadFileLink(sourceFile.size)
        insertLinkUploadEntity(uploadFileLink)
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.IO + job)
        coroutineScope.launch {
            splitUriToBlocks(
                uploadFileLink = uploadFileLink,
                uriString = uriString,
                isCancelled = { job.isCancelled },
            ).getOrThrow()
        }
        blockFolder.waitForContent(4)
        job.cancelAndJoin()
        blockFolder.children.forEach { file ->
            file.delete()
        }

        // When
        splitUriToBlocks(
            uploadFileLink = uploadFileLink,
            uriString = uriString,
            isCancelled = { false },
        ).getOrThrow()
    }

    @Test
    fun `if database contains entry and such file does not exists on filesystem both will be cleared`() = runTest {
        // Given
        val sourceFile = createFile(sourceFolder, 119.MiB)
        val uriString = Uri.fromFile(sourceFile).toString()
        val uploadFileLink = uploadFileLink(sourceFile.size)
        insertLinkUploadEntity(uploadFileLink)
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.IO + job)
        coroutineScope.launch {
            splitUriToBlocks(
                uploadFileLink = uploadFileLink,
                uriString = uriString,
                isCancelled = { job.isCancelled },
            ).getOrThrow()
        }
        blockFolder.waitForContent(4)
        job.cancelAndJoin()
        blockFolder.children.first().delete()

        // When
        splitUriToBlocks(
            uploadFileLink = uploadFileLink,
            uriString = uriString,
            isCancelled = { false },
        ).getOrNull()
        val rawBlocks = getRawUploadBlocks(uploadFileLink.id).getOrThrow()

        // Then
        assert(rawBlocks.isEmpty()) { "Actual blocks in database: ${rawBlocks.size}" }
        assert(blockFolder.children.isEmpty()) { "Actual blocks on file system: ${blockFolder.children.size}" }
    }

    private suspend fun insertLinkUploadEntity(uploadFileLink: UploadFileLink) {
        driveRule.db.linkUploadDao.insertOrUpdate(
            uploadFileLink.toLinkUploadEntity()
        )
    }

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
    )

    private fun File.waitForContent(contentSize: Int) {
        while (children.size < contentSize) {
            Thread.sleep(10)
        }
    }
}

internal fun createFile(parentFolder: File, size: Bytes): File =
    File(parentFolder, "${size.value}_byte(s)_${UUID.randomUUID()}.txt").apply {
        if (exists()) { delete() }
        createNewFile()
        if (size > 0.bytes) {
            RandomAccessFile(this, "rw").use { raf -> raf.setLength(size.value) }
        }
    }

internal val File.children: List<File> get() = listFiles()?.toList() ?: emptyList()
