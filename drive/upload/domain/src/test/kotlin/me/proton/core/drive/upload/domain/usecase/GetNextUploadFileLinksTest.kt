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

import androidx.core.net.toUri
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.StorageInfo
import me.proton.core.drive.base.domain.extension.GiB
import me.proton.core.drive.base.domain.extension.MiB
import me.proton.core.drive.base.domain.usecase.GetInternalStorageInfo
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.domain.entity.CacheOption
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileDescription
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.UpdateUploadState
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.usecase.storageInfo
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetNextUploadFileLinksTest {

    @get:Rule
    var driveRule = DriveRule(this)
    private lateinit var folderId: FolderId

    @get: Rule
    val temporaryFolder = TemporaryFolder()

    @Inject
    lateinit var getNextUploadFileLinks: GetNextUploadFileLinks

    @Inject
    lateinit var createUploadFile: CreateUploadFile

    @Inject
    lateinit var updateUploadState: UpdateUploadState

    @Inject
    lateinit var removeUploadFile: RemoveUploadFile

    @Inject
    lateinit var getInternalStorageInfo: GetInternalStorageInfo

    @Before
    fun setUp() = runTest {
        folderId = driveRule.db.photo { }
        getInternalStorageInfo.storageInfo = StorageInfo(total = 1.GiB, available = 10.MiB)
    }

    @Test
    fun none() = runTest {
        assertEquals(emptyList<UploadFileLink>(), getNextUploadFileLinks(userId).getOrThrow())
    }

    @Test
    fun `Given one slot running and no other links when get next should return nothing`() =
        runTest {
            createUploadFile(
                UploadFileLink.USER_PRIORITY,
                listOf(temporaryFolder.createFile(0, 1.MiB)),
            )

            val uploadFileLinks = getNextUploadFileLinks(userId).getOrThrow()

            updateUploadState(uploadFileLinks.map { it.id }.toSet(), UploadState.IDLE).getOrThrow()

            assertEquals(emptyList<UploadFileLink>(), getNextUploadFileLinks(userId).getOrThrow())
        }

    @Test
    fun `Given all slots running when get next should return nothing`() = runTest {
        createUploadFile(
            UploadFileLink.USER_PRIORITY,
            (0..9).map { index -> temporaryFolder.createFile(index, 1.MiB) },
        )

        val uploadFileLinks = getNextUploadFileLinks(userId).getOrThrow()

        updateUploadState(uploadFileLinks.map { it.id }.toSet(), UploadState.IDLE).getOrThrow()

        assertEquals(emptyList<UploadFileLink>(), getNextUploadFileLinks(userId).getOrThrow())
    }


    @Test
    fun `Given 10 user priority links when get next should return 6 links`() = runTest {
        createUploadFile(
            UploadFileLink.USER_PRIORITY,
            (0..9).map { index -> temporaryFolder.createFile(index, 1.MiB) },
        )

        val uploadFileLinks = getNextUploadFileLinks(userId).getOrThrow()

        assertEquals(listOf(
            UploadFileLink.USER_PRIORITY,
            UploadFileLink.USER_PRIORITY,
            UploadFileLink.USER_PRIORITY,
            UploadFileLink.USER_PRIORITY,
            UploadFileLink.USER_PRIORITY,
            UploadFileLink.USER_PRIORITY,
        ), uploadFileLinks.map { it.priority })
    }

    @Test
    fun `Given 10 backup priority links when get next should return 4 links`() = runTest {
        createUploadFile(
            UploadFileLink.BACKUP_PRIORITY,
            (0..9).map { index -> temporaryFolder.createFile(index, 1.MiB) },
        )

        val uploadFileLinks = getNextUploadFileLinks(userId).getOrThrow()

        assertEquals(listOf(
            UploadFileLink.BACKUP_PRIORITY,
            UploadFileLink.BACKUP_PRIORITY,
            UploadFileLink.BACKUP_PRIORITY,
            UploadFileLink.BACKUP_PRIORITY,
        ), uploadFileLinks.map { it.priority })
    }

    @Test
    fun `Given 10 backup and 2 user priority links when get next should return 6 links, 2 user and 4 backup`() =
        runTest {
            createUploadFile(
                UploadFileLink.BACKUP_PRIORITY,
                (0..9).map { index -> temporaryFolder.createFile(index, 1.MiB) },
            )
            createUploadFile(
                UploadFileLink.USER_PRIORITY,
                (10..11).map { index -> temporaryFolder.createFile(index, 1.MiB) },
            )

            val uploadFileLinks = getNextUploadFileLinks(userId).getOrThrow()

            assertEquals(listOf(
                UploadFileLink.USER_PRIORITY,
                UploadFileLink.USER_PRIORITY,
                UploadFileLink.BACKUP_PRIORITY,
                UploadFileLink.BACKUP_PRIORITY,
                UploadFileLink.BACKUP_PRIORITY,
                UploadFileLink.BACKUP_PRIORITY,
            ), uploadFileLinks.map { it.priority })
        }

    @Test
    fun `Given 3 small and 1 large links when get next should return only the small files`() =
        runTest {
            createUploadFile(
                UploadFileLink.USER_PRIORITY,
                listOf(
                    temporaryFolder.createFile(0, 1.MiB),
                    temporaryFolder.createFile(1, 1.MiB),
                    temporaryFolder.createFile(2, 1.MiB),
                    temporaryFolder.createFile(3, 9.MiB),
                )
            )

            val uploadFileLinks = getNextUploadFileLinks(userId).getOrThrow()

            assertEquals(listOf(
                1.MiB,
                1.MiB,
                1.MiB,
            ), uploadFileLinks.map { it.size })
        }

    @Test
    fun `Given 3 small idle and 1 large links when get next should return nothing`() = runTest {
        createUploadFile(
            UploadFileLink.USER_PRIORITY,
            listOf(
                temporaryFolder.createFile(0, 1.MiB),
                temporaryFolder.createFile(1, 1.MiB),
                temporaryFolder.createFile(2, 1.MiB),
                temporaryFolder.createFile(3, 9.MiB),
            )
        )

        val uploadFileLinks1 = getNextUploadFileLinks(userId).getOrThrow()
        updateUploadState(uploadFileLinks1.map { it.id }.toSet(), UploadState.IDLE).getOrThrow()

        val uploadFileLinks2 = getNextUploadFileLinks(userId).getOrThrow()
        assertEquals(emptyList<UploadFileLink>(), uploadFileLinks2)
    }

    @Test
    fun `Given 3 small uploading and 1 large links when get next should return nothing`() =
        runTest {
            createUploadFile(
                UploadFileLink.USER_PRIORITY,
                listOf(
                    temporaryFolder.createFile(0, 1.MiB),
                    temporaryFolder.createFile(1, 1.MiB),
                    temporaryFolder.createFile(2, 1.MiB),
                    temporaryFolder.createFile(3, 9.MiB),
                )
            )

            val uploadFileLinks1 = getNextUploadFileLinks(userId).getOrThrow()
            updateUploadState(
                uploadFileLinks1.map { it.id }.toSet(),
                UploadState.UPLOADING_BLOCKS
            ).getOrThrow()
            getInternalStorageInfo.storageInfo = StorageInfo(total = 1.GiB, available = 7.MiB)

            val uploadFileLinks2 = getNextUploadFileLinks(userId).getOrThrow()
            assertEquals(emptyList<UploadFileLink>(), uploadFileLinks2)
        }

    @Test
    fun `Given 1 large and 3 small links when get next should return only the large file`() =
        runTest {
            createUploadFile(
                UploadFileLink.USER_PRIORITY,
                listOf(
                    temporaryFolder.createFile(0, 9.MiB),
                    temporaryFolder.createFile(1, 1.MiB),
                    temporaryFolder.createFile(2, 1.MiB),
                    temporaryFolder.createFile(3, 1.MiB),
                )
            )

            val uploadFileLinks = getNextUploadFileLinks(userId).getOrThrow()

            assertEquals(listOf(
                9.MiB,
            ), uploadFileLinks.map { it.size })
        }

    @Test
    fun `Given 1 too large and 3 small links when get next should return only the small files`() =
        runTest {
            createUploadFile(
                UploadFileLink.USER_PRIORITY,
                listOf(
                    temporaryFolder.createFile(0, 20.MiB),
                    temporaryFolder.createFile(1, 1.MiB),
                    temporaryFolder.createFile(2, 1.MiB),
                    temporaryFolder.createFile(3, 1.MiB),
                )
            )

            val uploadFileLinks = getNextUploadFileLinks(userId).getOrThrow()

            assertEquals(listOf(
                1.MiB,
                1.MiB,
                1.MiB,
            ), uploadFileLinks.map { it.size })
        }

    @Test

    fun `Given only too large links and nothing uploading when get next should return only the first files`() =
        runTest {
            createUploadFile(
                UploadFileLink.USER_PRIORITY,
                listOf(
                    temporaryFolder.createFile(0, 20.MiB),
                    temporaryFolder.createFile(1, 19.MiB),
                )
            )

            val uploadFileLinks = getNextUploadFileLinks(userId).getOrThrow()

            assertEquals(listOf(
                20.MiB,
            ), uploadFileLinks.map { it.size })
        }

    private suspend fun createUploadFile(
        priority: Long,
        uriStrings: List<String>,
    ): List<UploadFileLink> = createUploadFile(
        userId = userId,
        volumeId = VolumeId(volumeId),
        parentId = folderId,
        uploadFileDescriptions = uriStrings.map { UploadFileDescription(it) },
        shouldDeleteSourceUri = false,
        networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
        shouldAnnounceEvent = false,
        cacheOption = CacheOption.NONE,
        priority = priority,
        shouldBroadcastErrorMessage = false
    ).getOrThrow()

    private fun TemporaryFolder.createFile(index: Int, size: Bytes): String =
        File(root, "photo${index}_${size.value}.jpg").apply {
            if (exists()) {
                delete()
            }
            createNewFile()
            if (size.value > 0L) {
                RandomAccessFile(this, "rw").use { it.setLength(size.value) }
            }
        }.toUri().path.let { path -> "file://$path" }


}
