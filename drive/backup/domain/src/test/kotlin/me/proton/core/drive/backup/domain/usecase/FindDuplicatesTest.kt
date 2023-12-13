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

package me.proton.core.drive.backup.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.data.repository.BackupDuplicateRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFileRepositoryImpl
import me.proton.core.drive.backup.data.repository.BackupFolderRepositoryImpl
import me.proton.core.drive.backup.data.repository.FolderFindDuplicatesRepository
import me.proton.core.drive.backup.domain.entity.BackupDuplicate
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.repository.FindDuplicatesRepository
import me.proton.core.drive.base.domain.entity.ClientUid
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.base.domain.repository.ClientUidRepository
import me.proton.core.drive.base.domain.usecase.CreateClientUid
import me.proton.core.drive.base.domain.usecase.CreateUuid
import me.proton.core.drive.base.domain.usecase.GetClientUid
import me.proton.core.drive.base.domain.usecase.GetOrCreateClientUid
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.shareId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.data.api.LinkApiDataSource
import me.proton.core.drive.link.data.api.response.CheckAvailableHashesResponse
import me.proton.core.drive.link.data.api.response.PendingHashDto
import me.proton.core.drive.link.data.repository.LinkRepositoryImpl
import me.proton.core.drive.link.domain.entity.CheckAvailableHashesInfo
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.entity.Link
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.network.domain.ApiException
import me.proton.core.network.domain.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class FindDuplicatesTest {
    @get:Rule
    val database = DriveDatabaseRule()

    private lateinit var folderId: FolderId
    private lateinit var clientUid: ClientUid

    private lateinit var findDuplicates: FindDuplicates

    private lateinit var findDuplicatesRepository: FindDuplicatesRepository
    private lateinit var backupFileRepository: BackupFileRepositoryImpl
    private lateinit var backupDuplicateRepository: BackupDuplicateRepositoryImpl
    private lateinit var linkApiDataSource: LinkApiDataSource

    private lateinit var backupFolder: BackupFolder

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive { }
        backupFolder = BackupFolder(
            bucketId = 0,
            folderId = folderId
        )
        val backupFolderRepository = BackupFolderRepositoryImpl(database.db)
        val addFolder = AddFolder(backupFolderRepository)
        addFolder(backupFolder)

        backupFileRepository = BackupFileRepositoryImpl(database.db)
        backupDuplicateRepository = BackupDuplicateRepositoryImpl(database.db)
        linkApiDataSource = mockk()
        val linkRepository = LinkRepositoryImpl(linkApiDataSource, database.db)
        findDuplicatesRepository = FolderFindDuplicatesRepository(linkRepository)
        val clientUidRepository = object : ClientUidRepository {
            private var clientUid: ClientUid? = null

            override suspend fun get(): ClientUid? = clientUid

            override suspend fun insert(clientUid: ClientUid) {
                this.clientUid = clientUid
            }

        }
        val uuid = UUID(0, 0)
        clientUid = uuid.toString()
        findDuplicates = FindDuplicates(
            configurationProvider = object : ConfigurationProvider {
                override val host: String = ""
                override val baseUrl: String = ""
                override val appVersionHeader: String = ""
                override val apiPageSize: Int = 2
            },
            findDuplicatesRepository = findDuplicatesRepository,
            backupFileRepository = backupFileRepository,
            backupDuplicateRepository = backupDuplicateRepository,
            getOrCreateClientUid = GetOrCreateClientUid(
                getClientUid = GetClientUid(clientUidRepository),
                createClientUid = CreateClientUid(
                    repository = clientUidRepository,
                    createUuid = CreateUuid { uuid }
                )
            )
        )
    }

    @Test
    fun empty() = runTest {

        val result = findDuplicates(userId, backupFolder)

        assertEquals(Result.success(Unit), result)
        assertEquals(
            emptyList<BackupFile>(),
            backupFileRepository.getAllFiles(userId, 0, 100),
        )
    }

    @Test
    fun duplicates() = runTest {
        backupFileRepository.insertFiles(
            userId = userId,
            backupFiles = listOf(
                backupFile(index = 0, backupFileState = BackupFileState.IDLE),
                backupFile(index = 1, backupFileState = BackupFileState.IDLE),
                backupFile(index = 2, backupFileState = BackupFileState.IDLE),
            )
        )

        coEvery { linkApiDataSource.checkAvailableHashes(folderId, any()) } answers {
            val info: CheckAvailableHashesInfo = secondArg()
            CheckAvailableHashesResponse(
                code = 1000,
                availableHashes = info.hashes.filter { hash -> hash == "hash0" },
                pendingHashDtos = info.hashes.filter { hash -> hash == "hash1" }.map { hash ->
                    PendingHashDto(
                        clientUid = info.clientUid,
                        hash = hash,
                        revisionId = "revision-id",
                        linkId = "link-id"
                    )
                }
            )
        }

        val result = findDuplicates(userId, backupFolder)

        assertEquals(Result.success(Unit), result)
        assertEquals(
            listOf(
                backupFile(index = 0, backupFileState = BackupFileState.READY),
                backupFile(index = 1, backupFileState = BackupFileState.READY),
                backupFile(index = 2, backupFileState = BackupFileState.POSSIBLE_DUPLICATE),
            ),
            backupFileRepository.getAllFiles(userId, 0, 100),
        )
        assertEquals(
            listOf(
                BackupDuplicate(
                    id = 1,
                    parentId = folderId,
                    hash = "hash1",
                    contentHash = null,
                    linkId = FileId(ShareId(userId, shareId), "link-id"),
                    linkState = Link.State.DRAFT,
                    revisionId = "revision-id",
                    clientUid = clientUid
                ),
                BackupDuplicate(
                    id = 2,
                    parentId = folderId,
                    hash = "hash2",
                    contentHash = null,
                    linkId = null,
                    linkState = Link.State.ACTIVE,
                    revisionId = null,
                    clientUid = clientUid
                ),
            ),
            backupDuplicateRepository.getAll(userId, folderId, 0, 100),
        )
    }

    @Test
    fun error() = runTest {
        backupFileRepository.insertFiles(
            userId = userId,
            backupFiles = listOf(
                backupFile(index = 0, backupFileState = BackupFileState.IDLE),
                backupFile(index = 1, backupFileState = BackupFileState.IDLE),
                backupFile(index = 2, backupFileState = BackupFileState.IDLE),
            )
        )

        val apiException = ApiException(
            error = ApiResult.Error.NoInternet()
        )
        coEvery { linkApiDataSource.checkAvailableHashes(folderId, any()) } throws apiException

        val result = findDuplicates(userId, backupFolder)

        assertEquals(Result.failure<Unit>(apiException), result)
        assertEquals(
            listOf(
                backupFile(index = 0, backupFileState = BackupFileState.IDLE),
                backupFile(index = 1, backupFileState = BackupFileState.IDLE),
                backupFile(index = 2, backupFileState = BackupFileState.IDLE),
            ),
            backupFileRepository.getAllFiles(userId, 0, 100),
        )
    }

    private fun backupFile(index: Int, backupFileState: BackupFileState) = backupFile(
        uriString = "uri$index",
        hash = "hash$index",
        backupFileState = backupFileState,
    )

    private fun backupFile(uriString: String, hash: String, backupFileState: BackupFileState) =
        BackupFile(
            bucketId = backupFolder.bucketId,
            uriString = uriString,
            mimeType = "",
            name = "",
            hash = hash,
            size = 0.bytes,
            state = backupFileState,
            date = TimestampS(0),
        )
}


