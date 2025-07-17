package me.proton.core.drive.photo.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.data.api.response.TagsMigrationResponse.Anchor
import me.proton.core.drive.photo.domain.entity.TagsMigrationAnchor
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatus
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getTagsMigrationStatus
import me.proton.core.drive.test.api.postTagsMigrationStatus
import me.proton.core.network.domain.ApiException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TagsMigrationRepositoryImplStatusTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var repository: TagsMigrationRepositoryImpl

    private lateinit var fileId: FileId
    private lateinit var file: TagsMigrationFile

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {
            fileId = file("photo-id")
        }
        file = TagsMigrationFile(
            volumeId = photoVolumeId,
            fileId = fileId,
            captureTime = TimestampS(1000),
        )
        repository.insertFiles(listOf(file))
    }

    @Test
    fun `getStatus finished false anchor null`() = runTest {
        driveRule.server.run {
            getTagsMigrationStatus(finished = false, anchor = null)
        }
        assertEquals(
            TagsMigrationStatus(finished = false, anchor = null),
            repository.getStatus(userId, photoVolumeId)
        )
    }

    @Test
    fun `getStatus finished true anchor not null`() = runTest {
        driveRule.server.run {
            getTagsMigrationStatus(
                finished = true, anchor = Anchor(
                    lastMigrationTimestamp = 5000,
                    lastProcessedLinkId = file.fileId.id,
                    lastProcessedCaptureTime = file.captureTime.value,
                    lastClientUid = "client-uid",
                )
            )
        }
        assertEquals(
            TagsMigrationStatus(
                finished = true, anchor = TagsMigrationAnchor(
                    currentTimestamp = TimestampS(5000),
                    lastProcessedLinkId = file.fileId,
                    lastProcessedCaptureTime = TimestampS(file.captureTime.value),
                    clientUid = "client-uid",
                )
            ),
            repository.getStatus(userId, photoVolumeId)
        )
    }

    @Test(expected = ApiException::class)
    fun `getStatus failed`() = runTest {
        driveRule.server.run {
            getTagsMigrationStatus { errorResponse() }
        }
        repository.getStatus(userId, photoVolumeId)
    }

    @Test
    fun updateStatus() = runTest {
        driveRule.server.run {
            postTagsMigrationStatus()
        }
        repository.updateStatus(
            userId, photoVolumeId, TagsMigrationStatus(
                finished = true, anchor = TagsMigrationAnchor(
                    currentTimestamp = TimestampS(5000),
                    lastProcessedLinkId = file.fileId,
                    lastProcessedCaptureTime = TimestampS(file.captureTime.value),
                    clientUid = "client-uid",
                )
            )
        )
    }
}
