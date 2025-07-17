/*
 * Copyright (c) 2023-2024 Proton AG.
 * This file is part of Proton Drive.
 *
 * Proton Drive is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Drive is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Drive.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.drive.photos.data.repository

import io.mockk.Called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.photos.domain.usecase.PhotoSyncLinkFolder
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.base.domain.provider.ConfigurationProvider
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.photo.data.api.PhotoApiDataSource
import me.proton.core.drive.photo.data.api.entity.PhotoListingDto
import me.proton.core.drive.photo.data.repository.PhotoRepositoryImpl
import me.proton.core.drive.share.crypto.domain.usecase.GetPhotoShare
import me.proton.core.drive.share.data.extension.toShare
import me.proton.core.drive.share.domain.entity.Share
import me.proton.core.drive.sorting.domain.entity.Direction
import me.proton.core.drive.stats.data.repository.UploadStatsRepositoryImpl
import me.proton.core.drive.stats.domain.entity.UploadStats
import me.proton.core.drive.stats.domain.usecase.GetUploadStats
import me.proton.core.drive.stats.domain.usecase.UpdateUploadStats
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoSyncLinkFolderTest {


    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var photoSyncLinkFolder: PhotoSyncLinkFolder
    private lateinit var updateUploadStats: UpdateUploadStats

    val pageSize = 5

    private val photoApiDataSource = mockk<PhotoApiDataSource>()

    @Before
    fun setUp() = runTest {
        val configurationProvider = object : ConfigurationProvider {
            override val host: String = ""
            override val baseUrl: String = ""
            override val appVersionHeader: String = ""
            override val apiListingPageSize: Int = pageSize
        }
        val getPhotoShare = mockk<GetPhotoShare> {
            every { this@mockk(any()) } answers {
                val userId: UserId = arg(0)
                database.db.shareDao.getAllFlow(userId).map { shares ->
                    shares.map { shareEntity -> shareEntity.toShare(userId) }
                        .firstOrNull { share -> share.type == Share.Type.PHOTO }?.let { share ->
                            DataResult.Success(ResponseSource.Local, share)
                        } ?: DataResult.Error.Local(null, null)
                }
            }
        }
        val repository = UploadStatsRepositoryImpl(database.db)
        updateUploadStats = UpdateUploadStats(repository)
        photoSyncLinkFolder = PhotoSyncLinkFolder(
            photoRepository = PhotoRepositoryImpl(photoApiDataSource, database.db),
            configurationProvider = configurationProvider,
            getPhotoShare = getPhotoShare,
            getUploadStats = GetUploadStats(repository)
        )
    }

    @Test
    fun `Given not photo share when sync should no do nothing`() = runTest {
        folderId = database.myFiles { }

        photoSyncLinkFolder(userId).getOrThrow()

        verify { photoApiDataSource wasNot Called }
    }

    @Test
    fun `Given photo share when sync should sync photo listing`() = runTest {
        folderId = database.photo { }

        val minimumFileCreationDateTime = TimestampS(12)
        updateUploadStats(
            UploadStats(
                folderId = folderId,
                count = 0,
                size = 0.bytes,
                minimumUploadCreationDateTime = TimestampS(0),
                minimumFileCreationDateTime = minimumFileCreationDateTime,
            )
        )

        coEvery {
            photoApiDataSource.getPhotoListings(
                userId = userId,
                volumeId = photoVolumeId,
                sortingDirection = Direction.DESCENDING,
                pageSize = pageSize,
                previousPageLastLinkId = any(),
                minimumCaptureTime = minimumFileCreationDateTime,
            )
        } answers {
            // will answer pages from with descending ids and captureTime from 100 to minimumCaptureTime

            val pageSize: Int = arg(3)
            val previousPageLastLinkId: String? = arg(4)
            val minimumCaptureTime: Long = arg(5)
            val previousPageLastId: Long = previousPageLastLinkId?.toLong() ?: 100L
            (1..pageSize).mapNotNull { index ->
                val id = previousPageLastId - index
                if (id >= minimumCaptureTime) {
                    PhotoListingDto(
                        linkId = id.toString(),
                        captureTime = id,
                    )
                } else {
                    null
                }
            }
        }

        photoSyncLinkFolder(userId).getOrThrow()

        assertEquals(
            88,
            database.db.photoListingDao.getPhotoListingCount(userId, photoVolumeId.id).first()
        )
    }
}
