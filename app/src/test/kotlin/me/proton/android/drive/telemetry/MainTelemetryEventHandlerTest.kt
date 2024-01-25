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
import kotlinx.coroutines.test.runTest
import me.proton.android.drive.usecase.GetShareAsPhotoShare
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.drive.announce.event.domain.entity.Event
import me.proton.core.drive.announce.event.domain.entity.Event.Upload.UploadState.UPLOAD_COMPLETE
import me.proton.core.drive.base.domain.entity.Percentage
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.bytes
import me.proton.core.drive.db.test.DriveDatabaseRule
import me.proton.core.drive.db.test.myDrive
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.linkupload.data.db.entity.LinkUploadEntity
import me.proton.core.drive.linkupload.data.extension.toUploadFileLink
import me.proton.core.drive.linkupload.domain.entity.UploadState
import me.proton.core.drive.linkupload.domain.usecase.GetUploadFileLink
import me.proton.core.drive.share.data.extension.toShare
import me.proton.core.drive.share.domain.entity.ShareId
import me.proton.core.drive.share.domain.usecase.GetShare
import me.proton.core.drive.stats.data.repository.BackupStatsRepositoryImpl
import me.proton.core.drive.stats.data.repository.UploadStatsRepositoryImpl
import me.proton.core.drive.stats.domain.entity.UploadStats
import me.proton.core.drive.stats.domain.usecase.GetUploadStats
import me.proton.core.drive.stats.domain.usecase.IsInitialBackup
import me.proton.core.drive.stats.domain.usecase.UpdateUploadStats
import me.proton.core.drive.telemetry.domain.entity.DriveTelemetryEvent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class MainTelemetryEventHandlerTest {
    @get:Rule
    val database = DriveDatabaseRule()
    private lateinit var folderId: FolderId

    private val userId = UserId("user-id")

    private val manager = StubbedDriveTelemetryManager()

    private lateinit var updateUploadStats: UpdateUploadStats
    private lateinit var handler: TelemetryEventHandler

    @Before
    fun setUp() = runTest {
        folderId = database.myDrive {}
        val getShare: GetShare = mockk()
        every { getShare(any(), any()) } answers {
            val shareId: ShareId = arg(0)
            database.db.shareDao.getFlow(shareId.userId, shareId.id).mapNotNull { shareEntity ->
                shareEntity?.toShare(shareId.userId)?.let {
                    DataResult.Success(ResponseSource.Local, it)
                }
            }
        }
        val getUploadFileLink: GetUploadFileLink = mockk()
        coEvery { getUploadFileLink(any<Long>()) } coAnswers {
            val id = arg<Long>(0)
            database.db.linkUploadDao.get(id)?.toUploadFileLink()?.let {
                DataResult.Success(ResponseSource.Local, it)
            } ?: DataResult.Error.Local("UploadFileLink with id $id is not found", null)
        }

        val uploadStatsRepository = UploadStatsRepositoryImpl(database.db)
        val backupStatsRepository = BackupStatsRepositoryImpl(database.db)
        updateUploadStats = UpdateUploadStats(uploadStatsRepository)

        val clock = { TimestampS(10) }
        val createPhotosEventBackupStopped = CreatePhotosEventBackupStopped(
            getUploadStats = GetUploadStats(uploadStatsRepository),
            clock = clock,
            isInitialBackup = IsInitialBackup(backupStatsRepository),
        )
        val getShareAsPhotoShare = GetShareAsPhotoShare(getShare)
        handler = TelemetryEventHandler(
            manager = manager,
            backupCompletedEventMapper = BackupCompletedEventMapper(
                getShareAsPhotoShare = getShareAsPhotoShare,
                createPhotosEventBackupStopped = createPhotosEventBackupStopped,
            ),
            backupEnabledEventMapper = BackupEnabledEventMapper(
                getShareAsPhotoShare = getShareAsPhotoShare,
            ),
            backupDisabledEventMapper = BackupDisabledEventMapper(
                getShareAsPhotoShare = getShareAsPhotoShare,
            ),
            backupStoppedEventMapper = BackupStoppedEventMapper(
                getShareAsPhotoShare = getShareAsPhotoShare,
                createPhotosEventBackupStopped = createPhotosEventBackupStopped,
            ),
            uploadEventMapper = UploadEventMapper(
                getUploadFileLink = getUploadFileLink,
                getShare = getShare,
                clock = clock
            ),
        )
    }


    @Test
    fun backupEnabled() = runTest {
        handler.onEvent(userId, Event.BackupEnabled(folderId))

        assertEquals(
            emptyMap<UserId, List<DriveTelemetryEvent>>(),
            manager.events,
        )
    }

    @Test
    fun backupDisabled() = runTest {
        handler.onEvent(userId, Event.BackupDisabled(folderId))

        assertEquals(
            emptyMap<UserId, List<DriveTelemetryEvent>>(),
            manager.events,
        )
    }

    @Test
    fun uploadDone() = runTest {
        val id = database.db.linkUploadDao.insert(
            LinkUploadEntity(
                userId = me.proton.core.drive.db.test.userId,
                volumeId = volumeId,
                shareId = folderId.shareId.id,
                parentId = folderId.id,
                name = "",
                uploadCreationDateTime = 0,
                size = 123 * 1024,
                state = UploadState.IDLE
            )
        )

        handler.onEvent(
            userId, Event.Upload(
                state = UPLOAD_COMPLETE,
                uploadFileLinkId = id,
                percentage = Percentage(100),
                shouldShow = false,
                reason = null,
            )
        )

        assertEquals(
            emptyMap<UserId, List<DriveTelemetryEvent>>(),
            manager.events,
        )
    }

    @Test
    fun backupState() = runTest {
        updateUploadStats(
            UploadStats(
                folderId = folderId,
                count = 10,
                size = 123.bytes,
                minimumUploadCreationDateTime = TimestampS(0),
                minimumFileCreationDateTime = null
            )
        )

        handler.onEvent(userId, Event.BackupCompleted(folderId))

        assertEquals(
            emptyMap<UserId, List<DriveTelemetryEvent>>(),
            manager.events,
        )
    }

    @Test
    fun backupStopped_FAILED() = runTest {
        updateUploadStats(
            UploadStats(
                folderId = folderId,
                count = 10,
                size = 123.bytes,
                minimumUploadCreationDateTime = TimestampS(0),
                minimumFileCreationDateTime = null
            )
        )

        handler.onEvent(
            userId, Event.BackupStopped(
                folderId = folderId,
                state = Event.Backup.BackupState.FAILED,
            )
        )

        assertEquals(
            emptyMap<UserId, List<DriveTelemetryEvent>>(),
            manager.events,
        )
    }
}
