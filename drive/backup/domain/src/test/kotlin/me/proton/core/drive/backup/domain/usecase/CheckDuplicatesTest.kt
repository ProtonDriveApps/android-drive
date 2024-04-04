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
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.BackupDuplicateRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.TimestampMs
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.crypto.domain.usecase.file.GetContentHash
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.key.domain.entity.Key
import me.proton.core.drive.key.domain.usecase.GetNodeKey
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.upload.domain.resolver.UriResolver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class CheckDuplicatesTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder

    private val getNodeKey = mockk<GetNodeKey>()
    private val getContentHash = mockk<GetContentHash>()

    private lateinit var checkDuplicates: CheckDuplicates

    private lateinit var backupFileRepository: BackupFileRepositoryImpl
    private lateinit var backupDuplicateRepository: BackupDuplicateRepositoryImpl

    @Before
    fun setUp() = runTest {

        folderId = database.myFiles { }

        val nodeKey: Key.Node = mockk()
        coEvery { getNodeKey(folderId) } returns Result.success(nodeKey)
        coEvery { getContentHash(folderId, nodeKey, any()) } answers {
            val input: String = arg(2)
            Result.success("getContentHash($input)")
        }

        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId
        )
        val backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        val addFolder = AddFolder(backupFolderRepository)
        addFolder(backupFolder).getOrThrow()
        backupFileRepository = BackupFileRepositoryImpl(database.db)
        backupDuplicateRepository = BackupDuplicateRepositoryImpl(database.db)

        checkDuplicates = CheckDuplicates(
            backupFileRepository = backupFileRepository,
            deleteFile = DeleteFile(backupFileRepository),
            backupDuplicateRepository = BackupDuplicateRepositoryImpl(database.db),
            uriResolver = object : UriResolver {
                override suspend fun <T> useInputStream(
                    uriString: String,
                    block: suspend (InputStream) -> T,
                ): T? = if (uriString == "missing") {
                    throw FileNotFoundException("Cannot found file with uri: $uriString")
                } else {
                    block(ByteArrayInputStream(uriString.toByteArray()))
                }

                override suspend fun getName(uriString: String): String? {
                    TODO("Not yet implemented")
                }

                override suspend fun getSize(uriString: String): Bytes? {
                    TODO("Not yet implemented")
                }

                override suspend fun getMimeType(uriString: String): String? {
                    TODO("Not yet implemented")
                }

                override suspend fun getLastModified(uriString: String): TimestampMs? {
                    TODO("Not yet implemented")
                }

                override suspend fun getUriInfo(uriString: String): UriResolver.UriInfo? {
                    TODO("Not yet implemented")
                }
            },
            getNodeKey = getNodeKey,
            getContentHash = getContentHash,
            configurationProvider = object : ConfigurationProvider {
                override val host: String = ""
                override val baseUrl: String = ""
                override val appVersionHeader: String = ""
                override val dbPageSize: Int = 1
            },
        )
    }

    @Test
    fun `Given duplicate without contentHash When checkDuplicates Should mark file as READY`() =
        runTest {
            backupFileRepository.insertFiles(
                listOf(backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE)),
            )
            val duplicates = listOf(backupDuplicate(1, "hash", null))
            backupDuplicateRepository.insertDuplicates(duplicates)

            checkDuplicates(userId, backupFolder).getOrThrow()

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

            checkDuplicates(userId, backupFolder).getOrThrow()

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
                        contentHash = "getContentHash(2c6d680f5c570ba21d22697cd028f230e9f4cd56)",
                    )
                )
            )

            checkDuplicates(userId, backupFolder).getOrThrow()

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
                        contentHash = "getContentHash(2c6d680f5c570ba21d22697cd028f230e9f4cd56)",
                    ),
                    backupDuplicate(
                        id = 2,
                        hash = "hash",
                        contentHash = "getContentHash(something-else-than-empty-hash)",
                    ),
                )
            )

            checkDuplicates(userId, backupFolder).getOrThrow()

            assertEquals(
                listOf(backupFile("hash", BackupFileState.DUPLICATED)),
                backupFileRepository.getAllFiles(folderId, 0, 100),
            )
            assertEquals(
                listOf(
                    backupDuplicate(
                        id = 2,
                        hash = "hash",
                        contentHash = "getContentHash(something-else-than-empty-hash)",
                    )
                ),
                backupDuplicateRepository.getAll(folderId, 0, 100),
            )
        }

    @Test
    fun `Given missing file When checkDuplicates Should delete it`() =
        runTest {
            backupFileRepository.insertFiles(
                listOf(backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "missing")),
            )

            checkDuplicates(userId, backupFolder).getOrThrow()

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
                    backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "uri1"),
                    backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "uri2"),
                ),
            )
            backupDuplicateRepository.insertDuplicates(
                listOf(
                    backupDuplicate(
                        id = 1,
                        hash = "hash",
                        contentHash = "getContentHash(eb73eba4eb87e7189c0246ff137854aa54c14dd2)",
                    ),
                    backupDuplicate(
                        id = 2,
                        hash = "hash",
                        contentHash = "getContentHash(a0d4696880dca9b25a7048aa4cf79e098aea6de7)",
                    ),
                )
            )

            checkDuplicates(userId, backupFolder).getOrThrow()

            assertEquals(
                listOf(
                    backupFile("hash", BackupFileState.DUPLICATED, "uri1"),
                    backupFile("hash", BackupFileState.DUPLICATED, "uri2"),
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
                    backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "uri1"),
                    backupFile("hash", BackupFileState.POSSIBLE_DUPLICATE, "uri2"),
                ),
            )
            backupDuplicateRepository.insertDuplicates(
                listOf(
                    backupDuplicate(
                        id = 1,
                        hash = "hash",
                        contentHash = "getContentHash(eb73eba4eb87e7189c0246ff137854aa54c14dd2)",
                    )
                )
            )

            checkDuplicates(userId, backupFolder).getOrThrow()

            assertEquals(
                listOf(
                    backupFile("hash", BackupFileState.DUPLICATED, "uri1"),
                    backupFile("hash", BackupFileState.READY, "uri2"),
                ),
                backupFileRepository.getAllFiles(folderId, 0, 100),
            )
            assertEquals(
                emptyList<BackupDuplicate>(),
                backupDuplicateRepository.getAll(folderId, 0, 100),
            )
        }

    private fun backupDuplicate(id : Long, hash: String, contentHash: String?) = BackupDuplicate(
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
        uriString: String = "uri",
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

