package me.proton.core.drive.photo.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.DOWNLOADED
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.IDLE
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.PREPARED
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.UPDATED
import me.proton.core.drive.photo.domain.entity.TagsMigrationStatistics
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TagsMigrationRepositoryImplTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var repository: TagsMigrationRepositoryImpl

    private lateinit var fileId1: FileId
    private lateinit var file1: TagsMigrationFile
    private lateinit var fileId2: FileId
    private lateinit var file2: TagsMigrationFile
    private lateinit var fileId3: FileId
    private lateinit var file3: TagsMigrationFile

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {
            fileId1 = file("photo-id-1")
            fileId2 = file("photo-id-2")
            fileId3 = file("photo-id-3")
        }
        file1 = TagsMigrationFile(
            volumeId = photoVolumeId,
            fileId = fileId1,
            captureTime = TimestampS(1000),
        )
        file2 = TagsMigrationFile(
            volumeId = photoVolumeId,
            fileId = fileId2,
            captureTime = TimestampS(2000),
        )
        file3 = TagsMigrationFile(
            volumeId = photoVolumeId,
            fileId = fileId3,
            captureTime = TimestampS(3000),
        )
    }

    @Test
    fun `file no found`() = runTest {
        assertNull(repository.getFile(photoVolumeId, fileId1))
    }

    @Test
    fun `update uri`() = runTest {
        repository.insertFiles(listOf(file1))

        repository.updateUri(photoVolumeId, fileId1, "uri")

        assertEquals(file1.copy(uriString = "uri"), repository.getFile(photoVolumeId, fileId1))
    }

    @Test
    fun `update state`() = runTest {
        repository.insertFiles(listOf(file1))

        repository.updateState(photoVolumeId, fileId1, UPDATED)

        assertEquals(file1.copy(state = UPDATED), repository.getFile(photoVolumeId, fileId1))
    }

    @Test
    fun `given no file when get tags should returns an empty list`() = runTest {
        assertEquals(emptySet<PhotoTag>(), repository.getTags(photoVolumeId, fileId1))
    }

    @Test
    fun `given no tags when get tags should returns an empty list`() = runTest {
        repository.insertFiles(listOf(file1))

        assertEquals(emptySet<PhotoTag>(), repository.getTags(photoVolumeId, fileId1))
    }

    @Test
    fun `get tags`() = runTest {
        val tags = setOf(PhotoTag.Raw, PhotoTag.Selfies)
        repository.insertFiles(listOf(file1))

        repository.insertTags(photoVolumeId, fileId1, tags)

        assertEquals(tags, repository.getTags(photoVolumeId, fileId1))
    }

    @Test
    fun `getFilesByState empty`() = runTest {
        repository.insertFiles(listOf(file1))

        repository.updateState(photoVolumeId, fileId1, PREPARED)

        assertEquals(
            emptyList<TagsMigrationFile>(),
            repository.getFilesByState(userId, photoVolumeId, IDLE, 100)
        )
    }

    @Test
    fun `getFilesByState found`() = runTest {
        repository.insertFiles(listOf(file1))

        repository.updateState(photoVolumeId, fileId1, PREPARED)

        assertEquals(
            listOf(file1.copy(state = PREPARED)),
            repository.getFilesByState(userId, photoVolumeId, PREPARED, 100)
        )
    }

    @Test
    fun `getBatchFilesByState empty`() = runTest {
        repository.insertFiles(listOf(file1))

        assertEquals(
            emptyList<TagsMigrationFile>(),
            repository.getBatchFilesByState(userId, photoVolumeId, IDLE, 100)
        )
    }

    @Test
    fun `getBatchFilesByState found`() = runTest {
        repository.insertFiles(listOf(file1, file2, file3))
        repository.updateUri(photoVolumeId, fileId1, "uri")
        repository.updateMimeType(photoVolumeId, fileId1, "image/jpg")
        repository.updateMimeType(photoVolumeId, fileId2, "video/mp4")
        repository.updateMimeType(photoVolumeId, fileId3, "image/jpg")

        assertEquals(
            listOf(
                file2.copy(mimeType = "video/mp4"),
                file1.copy(uriString = "uri", mimeType = "image/jpg"),
            ),
            repository.getBatchFilesByState(userId, photoVolumeId, IDLE, 100)
        )
    }

    @Test
    fun `getLatestFileByState not found`() = runTest {
        repository.insertFiles(listOf(file1, file2, file3))

        assertEquals(
            null,
            repository.getLatestFileByState(userId, photoVolumeId, PREPARED).first()
        )
    }

    @Test
    fun `getLatestFileByState found`() = runTest {
        repository.insertFiles(listOf(file1, file2, file3))

        assertEquals(
            file3,
            repository.getLatestFileByState(userId, photoVolumeId, IDLE).first()
        )
    }

    @Test
    fun `getOldestFileWithState not found`() = runTest {
        repository.insertFiles(listOf(file1, file2, file3))

        assertEquals(
            null,
            repository.getOldestFileWithState(userId, photoVolumeId, PREPARED).first()
        )
    }

    @Test
    fun `getOldestFileWithState found`() = runTest {
        repository.insertFiles(listOf(file1, file2, file3))

        assertEquals(
            file1,
            repository.getOldestFileWithState(userId, photoVolumeId, IDLE).first()
        )
    }

    @Test
    fun getStatistics() = runTest {
        repository.insertFiles(listOf(file1, file2, file3))


        repository.updateState(photoVolumeId, fileId1, PREPARED)
        repository.updateState(photoVolumeId, fileId2, PREPARED)
        repository.updateState(photoVolumeId, fileId3, DOWNLOADED)

        assertEquals(
            TagsMigrationStatistics(
                mapOf(
                    PREPARED to 2,
                    DOWNLOADED to 1,
                )
            ),
            repository.getStatistics(userId, photoVolumeId).first()
        )
    }

    @Test
    fun removeAll() = runTest {
        repository.insertFiles(listOf(file1, file2, file3))

        repository.removeAll(userId, photoVolumeId)

        assertEquals(
            emptyList<TagsMigrationFile>(),
            repository.getFilesByState(userId, photoVolumeId, IDLE, 100)
        )
    }
}
