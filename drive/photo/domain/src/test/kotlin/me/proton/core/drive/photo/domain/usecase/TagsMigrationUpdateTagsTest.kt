package me.proton.core.drive.photo.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.getOrNull
import me.proton.core.drive.base.domain.log.LogTag
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.IDLE
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.UPDATED
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.postLinkTags
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TagsMigrationUpdateTagsTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getTagsMigrationFile: GetTagsMigrationFile

    @Inject
    lateinit var getTagsMigrationFileTags: GetTagsMigrationFileTags

    @Inject
    lateinit var insertTagsMigrationFiles: InsertTagsMigrationFiles

    @Inject
    lateinit var insertTagsMigrationFileTags: InsertTagsMigrationFileTags

    @Inject
    lateinit var tagsMigrationUpdateTags: TagsMigrationUpdateTags

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
                )
            )
        ).getOrThrow()
        insertTagsMigrationFileTags(
            photoVolumeId, fileId, setOf(PhotoTag.Screenshots)
        ).getOrThrow()
    }

    @Test
    fun `happy path`() = runTest {
        driveRule.server.postLinkTags()

        tagsMigrationUpdateTags(photoVolumeId, fileId).getOrThrow()

        assertEquals(UPDATED, getTagsMigrationFile(photoVolumeId, fileId).getOrThrow()?.state)
    }

    @Test
    fun `server error`() = runTest {
        driveRule.server.postLinkTags { errorResponse() }

        tagsMigrationUpdateTags(photoVolumeId, fileId).getOrNull(LogTag.PHOTO, "Cannot update tags")

        assertEquals(IDLE, getTagsMigrationFile(photoVolumeId, fileId).getOrThrow()?.state)
    }

}
