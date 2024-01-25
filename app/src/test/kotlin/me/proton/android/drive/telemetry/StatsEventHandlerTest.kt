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

package me.proton.android.drive.telemetry

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.stats.BackupCompletedSideEffect
import me.proton.android.drive.stats.BackupStartedSideEffect
import me.proton.android.drive.stats.StatsEventHandler
import me.proton.android.drive.stats.UploadSideEffect
import me.proton.android.drive.usecase.GetShareAsPhotoShare
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.entity.Event.Upload.UploadState.UPLOAD_COMPLETE
import me.proton.core.drive.base.domain.entity.Bytes
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.data.extension.toUploadFileLink
import me.proton.core.drive.linkupload.data.factory.UploadBlockFactoryImpl
import me.proton.core.drive.linkupload.data.repository.LinkUploadRepositoryImpl
import me.proton.core.drive.linkupload.domain.entity.NetworkTypeProviderType
import me.proton.core.drive.linkupload.domain.entity.UploadFileLink
import me.proton.core.drive.linkupload.domain.repository.LinkUploadRepository
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.share.data.extension.toShare
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.stats.data.repository.BackupStatsRepositoryImpl
import me.proton.core.drive.stats.data.repository.UploadStatsRepositoryImpl
import me.proton.core.drive.stats.domain.entity.UploadStats
import me.proton.core.drive.stats.domain.usecase.DeleteUploadStats
import me.proton.core.drive.stats.domain.usecase.GetUploadStats
import me.proton.core.drive.stats.domain.usecase.IsInitialBackup
import me.proton.core.drive.stats.domain.usecase.SetOrIgnoreInitialBackup
import me.proton.core.drive.stats.domain.usecase.UpdateUploadStats
import me.proton.core.drive.volume.domain.entity.VolumeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class StatsEventHandlerTest {

    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private lateinit var linkUploadRepository: LinkUploadRepository
    private lateinit var getUploadStats: GetUploadStats
    private lateinit var isInitialBackup: IsInitialBackup

    private lateinit var handler: StatsEventHandler

    @Before
    fun setUp() = runTest {
        folderId = database.photo {}

        val getUploadFileLink: GetUploadFileLink = mockk()
        coEvery { getUploadFileLink(any<Long>()) } coAnswers {
            val id = arg<Long>(0)
            database.db.linkUploadDao.get(id)?.toUploadFileLink()?.let {
                DataResult.Success(ResponseSource.Local, it)
            } ?: DataResult.Error.Local("UploadFileLink with id $id is not found", null)
        }

        val uploadStatsRepository = UploadStatsRepositoryImpl(database.db)
        val backupStatsRepository = BackupStatsRepositoryImpl(database.db)
        val updateUploadStats = UpdateUploadStats(uploadStatsRepository)
        getUploadStats = GetUploadStats(uploadStatsRepository)
        isInitialBackup = IsInitialBackup(backupStatsRepository)

        val getShare: GetShare = mockk()
        every { getShare(any(), any()) } answers {
            val shareId: ShareId = arg(0)
            database.db.shareDao.getFlow(shareId.userId, shareId.id).mapNotNull { shareEntity ->
                shareEntity?.toShare(shareId.userId)?.let {
                    DataResult.Success(ResponseSource.Local, it)
                }
            }
        }

        linkUploadRepository = LinkUploadRepositoryImpl(database.db, UploadBlockFactoryImpl())

        val clock = { TimestampS(10) }
        handler = StatsEventHandler(
            backupCompletedSideEffect = BackupCompletedSideEffect(
                getShareAsPhotoShare = GetShareAsPhotoShare(getShare),
                deleteUploadStats = DeleteUploadStats(uploadStatsRepository),
                setOrIgnoreInitialBackup = SetOrIgnoreInitialBackup(backupStatsRepository),
            ),
            backupStartedSideEffect = BackupStartedSideEffect(
                getShareAsPhotoShare = GetShareAsPhotoShare(getShare),
                updateUploadStats = updateUploadStats,
                clock = clock,
            ),
            uploadSideEffect = UploadSideEffect(
                getUploadFileLink = getUploadFileLink,
                updateUploadStats = updateUploadStats,
            ),
        )
    }

    @Test
    fun started() = runTest {
        handler.onEvent(userId, Event.BackupStarted(folderId))

        assertEquals(
            UploadStats(
                folderId = folderId,
                count = 0,
                size = 0.bytes,
                minimumUploadCreationDateTime = TimestampS(10),
                minimumFileCreationDateTime = null
            ),
            getUploadStats(folderId).getOrThrow(),
        )
    }

    @Test
    fun uploads() = runTest {
        val uploadFileLinks = linkUploadRepository.insertUploadFileLinks(
            listOf(
                uploadFileLink(1, 1.bytes, TimestampS(11), TimestampS(5)),
                uploadFileLink(2, 2.bytes, TimestampS(12), TimestampS(6)),
                uploadFileLink(3, 3.bytes, TimestampS(13), TimestampS(7)),
            )
        )

        handler.onEvent(userId, Event.BackupStarted(folderId))
        uploadFileLinks.forEach { uploadFileLink ->
            handler.onEvent(
                userId,
                Event.Upload(
                    state = UPLOAD_COMPLETE,
                    uploadFileLinkId = uploadFileLink.id,
                    percentage = Percentage(1F),
                    shouldShow = false,
                    reason = null
                )
            )
        }

        assertEquals(
            UploadStats(
                folderId = folderId,
                count = 3,
                size = 6.bytes,
                minimumUploadCreationDateTime = TimestampS(10),
                minimumFileCreationDateTime = TimestampS(5)
            ),
            getUploadStats(folderId).getOrThrow(),
        )
    }

    @Test
    fun completed() = runTest {
        handler.onEvent(userId, Event.BackupStarted(folderId))
        handler.onEvent(userId, Event.BackupCompleted(folderId))

        assertThrows(NoSuchElementException::class.java) {
            runBlocking { getUploadStats(folderId).getOrThrow() }
        }
        assertFalse(isInitialBackup(folderId).getOrThrow())
    }

    private fun uploadFileLink(
        index: Long,
        size: Bytes,
        uploadCreationDateTime: TimestampS,
        fileCreationDateTime: TimestampS,
    ) = UploadFileLink(
        userId = userId,
        volumeId = VolumeId(volumeId),
        shareId = folderId.shareId,
        parentLinkId = folderId,
        uriString = "uri$index",
        name = "",
        mimeType = "",
        networkTypeProviderType = NetworkTypeProviderType.DEFAULT,
        priority = UploadFileLink.BACKUP_PRIORITY,
        size = size,
        uploadCreationDateTime = uploadCreationDateTime,
        fileCreationDateTime = fileCreationDateTime,
    )
}
