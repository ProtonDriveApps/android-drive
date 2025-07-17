package me.proton.core.drive.photo.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.data.api.response.TagsMigrationRequest
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.EXTRACTED
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.UPDATED
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.codeResponse
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.postTagsMigrationStatus
import me.proton.core.drive.test.api.request
import me.proton.core.network.domain.ApiException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TagsMigrationUpdateStatusTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getTagsMigrationFile: GetTagsMigrationFile

    @Inject
    lateinit var insertTagsMigrationFiles: InsertTagsMigrationFiles

    @Inject
    lateinit var updateTagsMigrationFileState: UpdateTagsMigrationFileState

    @Inject
    lateinit var tagsMigrationUpdateStatus: TagsMigrationUpdateStatus

    private lateinit var fileId: FileId

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {
            fileId = file("photo-id")
        }
        insertTagsMigrationFiles(
            listOf(
                TagsMigrationFile(
                    volumeId = photoVolumeId,
                    fileId = fileId,
                    captureTime = TimestampS(),
                    state = EXTRACTED
                )
            )
        ).getOrThrow()
    }

    @Test
    fun finished() = runTest {
        updateTagsMigrationFileState(photoVolumeId, fileId, UPDATED).getOrThrow()
        var request: TagsMigrationRequest? = null
        driveRule.server.postTagsMigrationStatus {
            request = request()
            codeResponse()
        }

        tagsMigrationUpdateStatus(photoVolumeId, fileId).getOrThrow()

        assertTrue(request!!.finished)
    }

    @Test
    fun ongoing() = runTest {
        var request: TagsMigrationRequest? = null
        driveRule.server.postTagsMigrationStatus {
            request = request()
            codeResponse()
        }

        tagsMigrationUpdateStatus(photoVolumeId, fileId).getOrThrow()

        assertFalse(request!!.finished)
    }

    @Test(expected = ApiException::class)
    fun `server error`() = runTest {
        driveRule.server.postTagsMigrationStatus { errorResponse() }

        tagsMigrationUpdateStatus(photoVolumeId, fileId).getOrThrow()
    }

}
