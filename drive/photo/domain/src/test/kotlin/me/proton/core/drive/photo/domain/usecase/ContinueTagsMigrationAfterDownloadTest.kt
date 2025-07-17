package me.proton.core.drive.photo.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.DOWNLOADED
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.UPDATED
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.manager.TestPhotoTagWorkManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ContinueTagsMigrationAfterDownloadTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getTagsMigrationFiles: GetTagsMigrationFiles

    @Inject
    lateinit var photoTagWorkManager: TestPhotoTagWorkManager

    @Inject
    lateinit var insertTagsMigrationFiles: InsertTagsMigrationFiles

    @Inject
    lateinit var continueTagsMigrationAfterDownload: ContinueTagsMigrationAfterDownload

    private lateinit var fileId: FileId
    private lateinit var file: TagsMigrationFile

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {
            fileId = file(id = "photo-id")
        }
        file = TagsMigrationFile(photoVolumeId, fileId, TimestampS(1000), DOWNLOADED)
        insertTagsMigrationFiles(listOf(file))
    }

    @Test
    fun `happy path`() = runTest {
        continueTagsMigrationAfterDownload(photoVolumeId, fileId).getOrThrow()

        assertEquals(
            listOf(file.copy(state = DOWNLOADED)),
            getTagsMigrationFiles(
                userId = userId,
                volumeId = photoVolumeId,
                state = DOWNLOADED,
                count = 100,
            ).getOrThrow()
        )
        assertEquals(listOf(fileId), photoTagWorkManager.tag)
    }
}
