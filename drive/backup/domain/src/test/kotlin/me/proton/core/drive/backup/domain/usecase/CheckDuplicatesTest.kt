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
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.getPublicAddressKeys
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CheckDuplicatesTest {

    @get:Rule
    val driveRule = DriveRule(this)
    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder

    @Inject
    lateinit var checkDuplicates: CheckDuplicates

    @Inject
    lateinit var backupFileRepository: BackupFileRepository

    @Inject
    lateinit var backupDuplicateRepository: BackupDuplicateRepository

    @Inject
    lateinit var addFolder: AddFolder

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.myFiles { }

        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId
        )
        addFolder(backupFolder).getOrThrow()
        driveRule.server.getPublicAddressKeys()
    }

    @Test
    fun `Given duplicate without contentHash When checkDuplicates Should mark file as READY`() =
        runTest {
            backupFileRepository.insertFiles(
                listOf(backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE)),
            )
            val duplicates = listOf(backupDuplicate(1, "hash", null))
            backupDuplicateRepository.insertDuplicates(duplicates)

            checkDuplicates(backupFolder).getOrThrow()

            assertEquals(
                listOf(backupFile("hash", BackupFileState.READY)),
                backupFileRepository.getAllFiles(folderId, 0, 100),
            )
            assertEquals(
                duplicates,
                backupDuplicateRepository.getAll(folderId, 0, 100),
            )
        }

    @Test
    fun `Given not matching content hash When checkDuplicates Should mark file as READY`() =
        runTest {
            backupFileRepository.insertFiles(
                listOf(backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE)),
            )
            val duplicates = listOf(
                backupDuplicate(
                    id = 1,
                    hash = "hash",
                    contentHash = "no-matching-content-hash",
                )
            )
            backupDuplicateRepository.insertDuplicates(
                duplicates
            )

            checkDuplicates(backupFolder).getOrThrow()

            assertEquals(
                listOf(backupFile("hash", BackupFileState.READY)),
                backupFileRepository.getAllFiles(folderId, 0, 100),
            )
            assertEquals(
                duplicates,
                backupDuplicateRepository.getAll(folderId, 0, 100),
            )
        }

    @Test
    fun `Given matching content hash When checkDuplicates Should mark file as DUPLICATED`() =
        runTest {
            backupFileRepository.insertFiles(
                listOf(backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE)),
            )
            backupDuplicateRepository.insertDuplicates(
                listOf(
                    backupDuplicate(
                        id = 1,
                        hash = "hash",
                        contentHash = "d736aa57fdbc8e1fd4c256e7737e2bcf55b8f6b0855ae0bfe84d4c1d148f6a53",
                    )
                )
            )

            checkDuplicates(backupFolder).getOrThrow()

            assertEquals(
                listOf(backupFile("hash", BackupFileState.DUPLICATED)),
                backupFileRepository.getAllFiles(folderId, 0, 100),
            )
            assertEquals(
                emptyList<BackupDuplicate>(),
                backupDuplicateRepository.getAll(folderId, 0, 100),
            )
        }

    @Test
    fun `Given not match and matching content hashes When checkDuplicates Should mark file as DUPLICATED`() =
        runTest {
            backupFileRepository.insertFiles(
                listOf(backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE)),
            )
            backupDuplicateRepository.insertDuplicates(
                listOf(
                    backupDuplicate(
                        id = 1,
                        hash = "hash",
                        contentHash = "d736aa57fdbc8e1fd4c256e7737e2bcf55b8f6b0855ae0bfe84d4c1d148f6a53",
                    ),
                    backupDuplicate(
                        id = 2,
                        hash = "hash",
                        contentHash = "something-else-than-empty-hash",
                    ),
                )
            )

            checkDuplicates(backupFolder).getOrThrow()

            assertEquals(
                listOf(backupFile("hash", BackupFileState.DUPLICATED)),
                backupFileRepository.getAllFiles(folderId, 0, 100),
            )
            assertEquals(
                listOf(
                    backupDuplicate(
                        id = 2,
                        hash = "hash",
                        contentHash = "something-else-than-empty-hash",
                    )
                ),
                backupDuplicateRepository.getAll(folderId, 0, 100),
            )
        }

    @Test
    fun `Given missing file When checkDuplicates Should delete it`() =
        runTest {
            backupFileRepository.insertFiles(
                listOf(backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "test://missing")),
            )

            checkDuplicates(backupFolder).getOrThrow()

            assertEquals(
                emptyList<BackupFile>(),
                backupFileRepository.getAllFiles(folderId, 0, 100),
            )
        }

    @Test
    fun `Given two different files with same name And both are duplicates When checkDuplicates Should marks file as DUPLICATED`() =
        runTest {
            backupFileRepository.insertFiles(
                listOf(
                    backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "test://uri1"),
                    backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "test://uri2"),
                ),
            )
            backupDuplicateRepository.insertDuplicates(
                listOf(
                    backupDuplicate(
                        id = 1,
                        hash = "hash",
                        contentHash = "cee843ef8c285d5c074dce68bc2e2ab19a5ecae881566610d78b948217236274",
                    ),
                    backupDuplicate(
                        id = 2,
                        hash = "hash",
                        contentHash = "db6105a58c8d20dc8461d1a158c42fc0454159e565989b58f89c85027371a23d",
                    ),
                )
            )

            checkDuplicates(backupFolder).getOrThrow()

            assertEquals(
                listOf(
                    backupFile("hash", BackupFileState.DUPLICATED, "test://uri1"),
                    backupFile("hash", BackupFileState.DUPLICATED, "test://uri2"),
                ),
                backupFileRepository.getAllFiles(folderId, 0, 100),
            )
            assertEquals(
                emptyList<BackupDuplicate>(),
                backupDuplicateRepository.getAll(folderId, 0, 100),
            )
        }

    @Test
    fun `Given two different files with same name And first is a duplicate name When checkDuplicates Should marks file as DUPLICATED`() =
        runTest {
            backupFileRepository.insertFiles(
                listOf(
                    backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "test://uri1"),
                    backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "test://uri2"),
                ),
            )
            backupDuplicateRepository.insertDuplicates(
                listOf(
                    backupDuplicate(
                        id = 1,
                        hash = "hash",
                        contentHash = "cee843ef8c285d5c074dce68bc2e2ab19a5ecae881566610d78b948217236274",
                    )
                )
            )

            checkDuplicates(backupFolder).getOrThrow()

            assertEquals(
                listOf(
                    backupFile("hash", BackupFileState.DUPLICATED, "test://uri1"),
                    backupFile("hash", BackupFileState.READY, "test://uri2"),
                ),
                backupFileRepository.getAllFiles(folderId, 0, 100),
            )
            assertEquals(
                emptyList<BackupDuplicate>(),
                backupDuplicateRepository.getAll(folderId, 0, 100),
            )
        }

    private fun backupDuplicate(id: Long, hash: String, contentHash: String?) = BackupDuplicate(
        id = id,
        parentId = folderId,
        hash = hash,
        contentHash = contentHash,
        linkId = FileId(folderId.shareId, "link-id"),
        linkState = Link.State.ACTIVE,
        revisionId = "revision-id",
        clientUid = ""
    )

    private fun backupFile(
        hash: String,
        backupFileState: BackupFileState,
        uriString: String = "test://uri",
    ) = BackupFile(
        bucketId = 0,
        folderId = folderId,
        uriString = uriString,
        mimeType = "",
        name = "",
        hash = hash,
        size = 0.bytes,
        state = backupFileState,
        date = TimestampS(0L),
    )
}

