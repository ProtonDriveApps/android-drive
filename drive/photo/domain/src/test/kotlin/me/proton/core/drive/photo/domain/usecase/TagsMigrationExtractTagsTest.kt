package me.proton.core.drive.photo.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.NullableLinkEntity
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.EXTRACTED
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TagsMigrationExtractTagsTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getTagsMigrationFile: GetTagsMigrationFile

    @Inject
    lateinit var getTagsMigrationFileTags: GetTagsMigrationFileTags

    @Inject
    lateinit var insertTagsMigrationFiles: InsertTagsMigrationFiles

    @Inject
    lateinit var tagsMigrationExtractTags: TagsMigrationExtractTags

    private lateinit var fileId: FileId

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {
            fileId = file(
                link = NullableLinkEntity(
                    id = "photo-id",
                    parentId = this.link.id,
                    type = 2L,
                    name = "Screenshot_xyz.png",
                )
            )
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
    }

    @Test
    fun `happy path`() = runTest {
        tagsMigrationExtractTags(photoVolumeId, fileId).getOrThrow()

        assertEquals(EXTRACTED, getTagsMigrationFile(photoVolumeId, fileId).getOrThrow()?.state)
        assertEquals(
            setOf(PhotoTag.Screenshots),
            getTagsMigrationFileTags(photoVolumeId, fileId).getOrThrow()
        )
    }

}
