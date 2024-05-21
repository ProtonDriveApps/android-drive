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
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.BackupDuplicateRepository
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.data.api.request.CheckAvailableHashesRequest
import me.proton.core.drive.link.data.api.response.CheckAvailableHashesResponse
import me.proton.core.drive.link.data.api.response.PendingHashDto
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.TestConfigurationProvider
import me.proton.core.drive.test.api.checkAvailableHashes
import me.proton.core.drive.test.api.jsonResponse
import me.proton.core.drive.test.api.request
import me.proton.core.drive.test.api.retryableErrorResponse
import me.proton.core.drive.test.usecase.StaticCreateUuid
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
class FindDuplicatesTest {
    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var findDuplicates: FindDuplicates

    @Inject
    lateinit var backupFileRepository: BackupFileRepository

    @Inject
    lateinit var backupDuplicateRepository: BackupDuplicateRepository

    @Inject
    lateinit var configurationProvider: TestConfigurationProvider

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }
        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId
        )
        addFolder(backupFolder).getOrThrow()
        configurationProvider.apiPageSize = 2
    }

    @Test
    fun empty() = runTest {

        findDuplicates(backupFolder).getOrThrow()

        assertEquals(
            emptyList<BackupFile>(),
            backupFileRepository.getAllFiles(folderId, 0, 100),
        )
    }

    @Test
    fun duplicates() = runTest {
        backupFileRepository.insertFiles(
            backupFiles = listOf(
                backupFile(index = 2, backupFileState = BackupFileState.IDLE),
                backupFile(index = 1, backupFileState = BackupFileState.IDLE),
                backupFile(index = 0, backupFileState = BackupFileState.IDLE),
            )
        )

        driveRule.server.run {
            checkAvailableHashes {
                jsonResponse {
                    val request = request<CheckAvailableHashesRequest>()
                    CheckAvailableHashesResponse(
                        code = 1000,
                        availableHashes = request.hashes.filter { hash -> hash == "hash0" },
                        pendingHashDtos = request.hashes.filter { hash -> hash == "hash1" }
                            .map { hash ->
                                PendingHashDto(
                                    clientUid = request.clientUid.orEmpty().firstOrNull(),
                                    hash = hash,
                                    revisionId = "revision-id",
                                    linkId = "link-id"
                                )
                            }
                    )
                }
            }
        }

        findDuplicates(backupFolder).getOrThrow()

        assertEquals(
            listOf(
                backupFile(index = 2, backupFileState = BackupFileState.POSSIBLE_DUPLICATE),
                backupFile(index = 1, backupFileState = BackupFileState.READY),
                backupFile(index = 0, backupFileState = BackupFileState.READY),
            ),
            backupFileRepository.getAllFiles(folderId, 0, 100),
        )
        assertEquals(
            listOf(
                BackupDuplicate(
                    id = 1,
                    parentId = folderId,
                    hash = "hash2",
                    contentHash = null,
                    linkId = null,
                    linkState = Link.State.ACTIVE,
                    revisionId = null,
                    clientUid = StaticCreateUuid.uuid.toString()
                ),
                BackupDuplicate(
                    id = 2,
                    parentId = folderId,
                    hash = "hash1",
                    contentHash = null,
                    linkId = FileId(folderId.shareId, "link-id"),
                    linkState = Link.State.DRAFT,
                    revisionId = "revision-id",
                    clientUid = StaticCreateUuid.uuid.toString()
                ),
            ),
            backupDuplicateRepository.getAll(folderId, 0, 100).sortedBy { it.id },
        )
    }

    @Test
    fun error() = runTest {
        backupFileRepository.insertFiles(
            backupFiles = listOf(
                backupFile(index = 2, backupFileState = BackupFileState.IDLE),
                backupFile(index = 1, backupFileState = BackupFileState.IDLE),
                backupFile(index = 0, backupFileState = BackupFileState.IDLE),
            )
        )

        driveRule.server.checkAvailableHashes { retryableErrorResponse() }

        val result = findDuplicates(backupFolder)
        assertThrows(ApiException::class.java) { result.getOrThrow() }

        assertEquals(
            listOf(
                backupFile(index = 2, backupFileState = BackupFileState.IDLE),
                backupFile(index = 1, backupFileState = BackupFileState.IDLE),
                backupFile(index = 0, backupFileState = BackupFileState.IDLE),
            ),
            backupFileRepository.getAllFiles(folderId, 0, 100),
        )
    }

    private fun backupFile(index: Int, backupFileState: BackupFileState) = backupFile(
        uriString = "uri$index",
        hash = "hash$index",
        backupFileState = backupFileState,
        date = TimestampS(index.toLong())
    )

    private fun backupFile(
        uriString: String,
        hash: String,
        backupFileState: BackupFileState,
        date: TimestampS = TimestampS(0),
    ) =
        BackupFile(
            bucketId = backupFolder.bucketId,
            folderId = folderId,
            uriString = uriString,
            mimeType = "",
            name = "",
            hash = hash,
            size = 0.bytes,
            state = backupFileState,
            date = date,
        )
}


