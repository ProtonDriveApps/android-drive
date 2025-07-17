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
class StopTagsMigrationTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getTagsMigrationFiles: GetTagsMigrationFiles

    @Inject
    lateinit var photoTagWorkManager: TestPhotoTagWorkManager

    @Inject
    lateinit var insertTagsMigrationFiles: InsertTagsMigrationFiles

    @Inject
    lateinit var stopTagsMigration: StopTagsMigration

    private lateinit var fileId1: FileId
    private lateinit var file1: TagsMigrationFile
    private lateinit var fileId2: FileId
    private lateinit var file2: TagsMigrationFile
    private lateinit var fileId3: FileId
    private lateinit var file3: TagsMigrationFile

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {
            fileId1 = file(id = "photo-id-1")
            fileId2 = file(id = "photo-id-2")
            fileId3 = file(id = "photo-id-3")
        }
        file1 = TagsMigrationFile(photoVolumeId, fileId1, TimestampS(1000))
        file2 = TagsMigrationFile(photoVolumeId, fileId2, TimestampS(2000))
        file3 = TagsMigrationFile(photoVolumeId, fileId3, TimestampS(3000))
        insertTagsMigrationFiles(listOf(file1, file2, file3))
    }

    @Test
    fun `happy path`() = runTest {
        stopTagsMigration(userId, photoVolumeId).getOrThrow()

        assertEquals(emptyList<TagsMigrationFile>(), getIdleFiles())
        assertEquals(listOf(photoVolumeId), photoTagWorkManager.cancel)
    }

    private suspend fun getIdleFiles() = getTagsMigrationFiles(
        userId = userId,
        volumeId = photoVolumeId,
        state = TagsMigrationFile.State.IDLE,
        count = 100,
    ).getOrThrow()
}
