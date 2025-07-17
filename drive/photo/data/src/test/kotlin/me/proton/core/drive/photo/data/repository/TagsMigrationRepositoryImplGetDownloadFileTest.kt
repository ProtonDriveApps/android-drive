package me.proton.core.drive.photo.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.linkdownload.domain.entity.DownloadState
import me.proton.core.drive.linkdownload.domain.usecase.SetDownloadState
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.DOWNLOADED
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.PREPARED
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
class TagsMigrationRepositoryImplGetDownloadFileTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var setDownloadState: SetDownloadState

    @Inject
    lateinit var repository: TagsMigrationRepositoryImpl

    private lateinit var fileId1: FileId
    private lateinit var file1: TagsMigrationFile

    private lateinit var fileId2: FileId
    private lateinit var file2: TagsMigrationFile

    @Before
    fun setUp() = runTest {
        driveRule.db.photo {
            fileId1 = file("photo-id-1")
            fileId2 = file("photo-id-2")
        }
        file1 = TagsMigrationFile(
            volumeId = photoVolumeId,
            fileId = fileId1,
            captureTime = TimestampS(10000),
        )
        file2 = TagsMigrationFile(
            volumeId = photoVolumeId,
            fileId = fileId2,
            captureTime = TimestampS(2000),
        )
        repository.insertFiles(listOf(file1))
        repository.insertFiles(listOf(file2))
    }

    @Test
    fun `no downloading file`() = runTest {
        assertNull(repository.getLatestDownloadedFile(userId, photoVolumeId).first()?.fileId)
    }

    @Test
    fun `no downloaded file`() = runTest {
        repository.updateState(photoVolumeId, fileId1, PREPARED)

        assertNull(repository.getLatestDownloadedFile(userId, photoVolumeId).first()?.fileId)
    }

    @Test
    fun `one downloaded file`() = runTest {
        setDownloadState(fileId1, DownloadState.Downloaded())
        repository.updateState(photoVolumeId, fileId1, PREPARED)

        assertEquals(fileId1, repository.getLatestDownloadedFile(userId, photoVolumeId).first()?.fileId)
    }

    @Test
    fun `given multiple files downloaded when get downloaded file then returns only the newest file`() =
        runTest {
            repository.updateState(photoVolumeId, fileId1, PREPARED)
            setDownloadState(fileId1, DownloadState.Downloaded())
            repository.updateState(photoVolumeId, fileId2, PREPARED)
            setDownloadState(fileId2, DownloadState.Downloaded())

            assertEquals(fileId1, repository.getLatestDownloadedFile(userId, photoVolumeId).first()?.fileId)
        }

    @Test
    fun `given multiple files downloaded when get downloadeded file then returns only the newest downloading file`() =
        runTest {
            setDownloadState(fileId1, DownloadState.Downloaded())
            repository.updateState(photoVolumeId, fileId1, DOWNLOADED)
            repository.updateState(photoVolumeId, fileId2, PREPARED)
            setDownloadState(fileId2, DownloadState.Downloaded())

            assertEquals(fileId2, repository.getLatestDownloadedFile(userId, photoVolumeId).first()?.fileId)
        }

    @Test
    fun `given database changes when get downloaded file should not returns duplicates`() = runTest {
        val fileIds = mutableListOf<FileId?>()
        val job = repository.getLatestDownloadedFile(userId, photoVolumeId)
            .take(5)
            .map { it?.fileId }
            .onEach { fileId ->
                fileIds.add(fileId)
                fileId?.let{
                    repository.updateState(photoVolumeId, fileId, DOWNLOADED)
                }
            }
            .launchIn(this)

        repository.updateState(photoVolumeId, fileId1, PREPARED)
        setDownloadState(fileId1, DownloadState.Downloaded())
        repository.updateState(photoVolumeId, fileId2, PREPARED)
        setDownloadState(fileId2, DownloadState.Downloaded())

        job.join()

        assertEquals(
            listOf(
                null,
                fileId1,
                null,
                fileId2,
                null,
            ), fileIds
        )
    }
}
