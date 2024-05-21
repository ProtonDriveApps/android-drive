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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.backup.domain.entity.BackupError
import me.proton.core.drive.backup.domain.entity.BackupFile
import me.proton.core.drive.backup.domain.entity.BackupFileState
import me.proton.core.drive.backup.domain.entity.BackupFolder
import me.proton.core.drive.backup.domain.manager.StubbedBackupManager
import me.proton.core.drive.backup.domain.manager.StubbedUploadWorkManager
import me.proton.core.drive.backup.domain.repository.BackupFileRepository
import me.proton.core.drive.base.domain.entity.StorageInfo
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.db.test.NullableUserEntity
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.link.domain.extension.userId
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadBulk
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.TestConfigurationProvider
import me.proton.core.drive.test.usecase.TestGetInternalStorageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class UploadFolderTest {
    @get:Rule
    val driveRule = DriveRule(this)

    private lateinit var folderId: FolderId
    private lateinit var backupFolder: BackupFolder
    private lateinit var backupFile: BackupFile
    private val bucketId = 0

    @Inject
    lateinit var uploadFolder: UploadFolder

    @Inject
    lateinit var getErrors: GetErrors

    @Inject
    lateinit var backupManager: StubbedBackupManager

    @Inject
    lateinit var backupFileRepository: BackupFileRepository

    @Inject
    lateinit var linkUploadRepository: LinkUploadRepository

    @Inject
    lateinit var addFolder: AddFolder

    @Inject
    lateinit var uploadWorkManager: StubbedUploadWorkManager

    @Inject
    lateinit var configurationProvider: TestConfigurationProvider

    @Inject
    lateinit var getInternalStorageInfo: TestGetInternalStorageInfo

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.user(
            NullableUserEntity(
                maxSpace = 50.MiB.value,
                usedSpace = 0.MiB.value,
            )
        ) {
            volume {
                mainShare { }
            }
        }
        getInternalStorageInfo.storageInfo = StorageInfo(100.GiB, 80.GiB)
        configurationProvider.uploadLimitThreshold = 5
        backupFile = NullableBackupFile(
            bucketId = bucketId,
            folderId = folderId,
            uriString = "uri",
            state = BackupFileState.READY,
        )
        backupFolder = BackupFolder(bucketId, folderId)
        addFolder(backupFolder).getOrThrow()
    }

    @Test
    fun `Given no file when upload should do nothing`() = runTest {

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(emptyMap<FolderId, UploadBulk>(), uploadWorkManager.bulks)
    }

    @Test
    fun `Given one file when upload should one file`() = runTest {
        backupFileRepository.insertFiles(listOf(backupFile))

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(
            mapOf(
                folderId to defaultUploadBulk(
                    listOf(UploadFileDescription("uri")),
                    CacheOption.THUMBNAIL_DEFAULT
                )
            ), uploadWorkManager.bulks
        )
    }

    @Test
    fun `Given one file already uploading when upload should do nothing`() = runTest {
        backupFileRepository.insertFiles(listOf(backupFile))

        linkUploadRepository.insertUploadFileLink(
            uploadFileLink("uri")
        )

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(emptyMap<FolderId, UploadBulk>(), uploadWorkManager.bulks)
    }

    @Test
    fun `Given one file already enqueued when upload should do nothing`() = runTest {
        backupFileRepository.insertFiles(
            listOf(backupFile.copy(state = BackupFileState.ENQUEUED))
        )

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(emptyMap<FolderId, UploadBulk>(), uploadWorkManager.bulks)
    }

    @Test
    fun `Given one file already uploaded when upload should delete it`() = runTest {
        backupFileRepository.insertFiles(
            listOf(backupFile.copy(state = BackupFileState.COMPLETED))
        )

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(emptyMap<FolderId, UploadBulk>(), uploadWorkManager.bulks)
    }

    @Test
    fun `Given one failed file when upload should one file`() = runTest {
        backupFileRepository.insertFiles(
            listOf(backupFile.copy(state = BackupFileState.FAILED)),
        )

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(
            mapOf(
                folderId to defaultUploadBulk(
                    uploadFileDescriptions = listOf(UploadFileDescription("uri")),
                    cacheOption = CacheOption.THUMBNAIL_DEFAULT
                )
            ), uploadWorkManager.bulks
        )
    }
    @Test
    fun `Given one failed file with limited storage when upload should one file without caching it`() = runTest {
        getInternalStorageInfo.storageInfo = StorageInfo(1.GiB, 200.MiB)
        backupFileRepository.insertFiles(
            listOf(backupFile.copy(state = BackupFileState.FAILED)),
        )

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(
            mapOf(
                folderId to defaultUploadBulk(
                    uploadFileDescriptions = listOf(UploadFileDescription("uri")),
                    cacheOption = CacheOption.NONE
                )
            ), uploadWorkManager.bulks
        )
    }

    @Test
    fun `Given one failed file with max attempts when upload should do nothing`() = runTest {
        backupFileRepository.insertFiles(
            listOf(backupFile.copy(state = BackupFileState.FAILED, attempts = 5)),
        )

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(emptyMap<FolderId, UploadBulk>(), uploadWorkManager.bulks)
    }

    @Test
    fun `Given ten files when upload should only upload five of them`() = runTest {
        backupFileRepository.insertFiles((0..9).map { index ->
            NullableBackupFile(
                bucketId = bucketId,
                folderId = folderId,
                uriString = "uri$index",
                state = BackupFileState.READY,
            )
        })

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(
            mapOf(
                folderId to defaultUploadBulk(
                    uploadFileDescriptions = (0..4).map { index -> UploadFileDescription("uri$index") },
                    cacheOption = CacheOption.THUMBNAIL_DEFAULT
                )
            ), uploadWorkManager.bulks
        )
    }

    @Test
    fun `Given ten files and two uploading when upload should only upload two of them`() = runTest {
        backupFileRepository.insertFiles((0..9).map { index ->
            NullableBackupFile(
                bucketId = bucketId,
                folderId = folderId,
                uriString = "uri$index",
                state = BackupFileState.READY,
            )
        })

        linkUploadRepository.insertUploadFileLinks(
            listOf(
                uploadFileLink("uri0"),
                uploadFileLink("uri1"),
            )
        )

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(
            mapOf(
                folderId to defaultUploadBulk(
                    uploadFileDescriptions = (2..4).map { index -> UploadFileDescription("uri$index") },
                    cacheOption = CacheOption.THUMBNAIL_DEFAULT
                )
            ), uploadWorkManager.bulks
        )
    }

    @Test
    fun `Given ten files of 5MiB when upload should only upload five of them`() = runTest {
        backupFileRepository.insertFiles((0..9).map { index ->
            NullableBackupFile(
                bucketId = bucketId,
                folderId = folderId,
                uriString = "uri$index",
                size = 5.MiB,
                state = BackupFileState.READY,
            )
        })

        uploadFolder(backupFolder).getOrThrow()

        assertEquals(
            mapOf(
                folderId to defaultUploadBulk(
                    uploadFileDescriptions = (0..4).map { index -> UploadFileDescription("uri$index") },
                    cacheOption = CacheOption.THUMBNAIL_DEFAULT
                )
            ), uploadWorkManager.bulks
        )
    }

    @Test
    fun `Given a file too big for user space when upload should stop backup`() = runTest {
        backupFileRepository.insertFiles(
            listOf(
                NullableBackupFile(
                    bucketId = bucketId,
                    folderId = folderId,
                    uriString = "uri",
                    size = 100.MiB,
                    state = BackupFileState.READY,
                )
            )
        )

        uploadFolder(backupFolder).getOrThrow()

        assertTrue(backupManager.stopped)
        assertEquals(
            listOf(BackupError.DriveStorage()),
            getErrors(folderId).first(),
        )
    }

    private fun uploadFileLink(uriString: String) = UploadFileLink(
        userId = userId,
        volumeId = volumeId,
        shareId = folderId.shareId,
        parentLinkId = folderId,
        uriString = uriString,
        name = "",
        mimeType = "",
        networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
        priority = UploadFileLink.USER_PRIORITY,
    )

    private fun defaultUploadBulk(
        uploadFileDescriptions: List<UploadFileDescription>,
        cacheOption: CacheOption
    ) = UploadBulk(
        id = 1,
        userId = folderId.userId,
        volumeId = volumeId,
        shareId = folderId.shareId,
        parentLinkId = folderId,
        uploadFileDescriptions = uploadFileDescriptions,
        shouldDeleteSourceUri = false,
        networkTypeProviderType = NetworkTypeProviderType.BACKUP,
        shouldAnnounceEvent = false,
        cacheOption = cacheOption,
        priority = UploadFileLink.BACKUP_PRIORITY,
        shouldBroadcastErrorMessage = false
    )
}
