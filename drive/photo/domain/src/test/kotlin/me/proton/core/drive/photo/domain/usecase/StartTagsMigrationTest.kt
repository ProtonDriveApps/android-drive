package me.proton.core.drive.photo.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.repository.ClientUidRepository
import me.proton.core.drive.db.test.file
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.DISABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.ENABLED
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlag.State.NOT_FOUND
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_PHOTOS_TAGS_MIGRATION
import me.proton.core.drive.feature.flag.domain.entity.FeatureFlagId.Companion.DRIVE_PHOTOS_TAGS_MIGRATION_DISABLED
import me.proton.core.drive.link.domain.entity.FileId
import me.proton.core.drive.photo.data.api.entity.PhotoListingDto
import me.proton.core.drive.photo.data.api.response.TagsMigrationResponse
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile
import me.proton.core.drive.photo.domain.entity.TagsMigrationFile.State.PREPARED
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getPhotoListings
import me.proton.core.drive.test.api.getTagsMigrationStatus
import me.proton.core.drive.test.manager.TestPhotoTagWorkManager
import me.proton.core.drive.test.repository.TestFeatureFlagRepository.Companion.flags
import me.proton.core.drive.volume.domain.entity.VolumeId
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
class StartTagsMigrationTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getTagsMigrationFiles: GetTagsMigrationFiles

    @Inject
    lateinit var photoTagWorkManager: TestPhotoTagWorkManager

    @Inject
    lateinit var clientUidRepository: ClientUidRepository

    @Inject
    lateinit var updateTagsMigrationFileState: UpdateTagsMigrationFileState

    @Inject
    lateinit var startTagsMigration: StartTagsMigration

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
        clientUidRepository.insert("test-client-id")
        flags[DRIVE_PHOTOS_TAGS_MIGRATION_DISABLED] = NOT_FOUND
        flags[DRIVE_PHOTOS_TAGS_MIGRATION] = ENABLED
    }

    @Test
    fun `given migration kill switch when start migration then should do nothing`() = runTest {
        flags[DRIVE_PHOTOS_TAGS_MIGRATION_DISABLED] = ENABLED

        startTagsMigration(userId, photoVolumeId).getOrThrow()

        assertEquals(emptyList<TagsMigrationFile>(), getIdleFiles())
        assertEquals(emptyList<VolumeId>(), photoTagWorkManager.enqueue)
    }

    @Test
    fun `given migration feature flag off when start migration then should do nothing`() = runTest {
        flags[DRIVE_PHOTOS_TAGS_MIGRATION] = DISABLED

        startTagsMigration(userId, photoVolumeId).getOrThrow()

        assertEquals(emptyList<TagsMigrationFile>(), getIdleFiles())
        assertEquals(emptyList<VolumeId>(), photoTagWorkManager.enqueue)
    }

    @Test(expected = ApiException::class)
    fun `given api error when start migration then should throw error`() = runTest {
        driveRule.server.run {
            getTagsMigrationStatus { errorResponse() }
        }

        startTagsMigration(userId, photoVolumeId).getOrThrow()
    }

    @Test
    fun `given finished migration when start migration then should do nothing`() = runTest {
        driveRule.server.run {
            getTagsMigrationStatus(finished = true)
        }

        startTagsMigration(userId, photoVolumeId).getOrThrow()

        assertEquals(emptyList<TagsMigrationFile>(), getIdleFiles())
        assertEquals(emptyList<VolumeId>(), photoTagWorkManager.enqueue)
    }

    @Test
    fun `given migration started by another client when start migration then should do nothing`() =
        runTest {
            driveRule.server.run {
                getTagsMigrationStatus(
                    finished = false, anchor = TagsMigrationResponse.Anchor(
                        lastProcessedLinkId = file2.fileId.id,
                        lastProcessedCaptureTime = file2.captureTime.value,
                        lastMigrationTimestamp = 10000,
                        lastClientUid = "another-client-id"
                    )
                )
            }

            startTagsMigration(userId, photoVolumeId).getOrThrow()

            assertEquals(emptyList<TagsMigrationFile>(), getIdleFiles())
            assertEquals(emptyList<VolumeId>(), photoTagWorkManager.enqueue)
        }

    @Test
    fun `given no migration and no photos when start migration then should do nothing`() = runTest {
        driveRule.server.run {
            getTagsMigrationStatus()
            getPhotoListings()
        }

        startTagsMigration(userId, photoVolumeId).getOrThrow()

        assertEquals(emptyList<TagsMigrationFile>(), getIdleFiles())
        assertEquals(emptyList<VolumeId>(), photoTagWorkManager.enqueue)
    }

    @Test
    fun `given no migration and photos when start migration then should start migration`() =
        runTest {
            driveRule.server.run {
                getTagsMigrationStatus()
                getPhotoListings(listOf(file3.toDto(), file2.toDto(), file1.toDto()))
            }

            startTagsMigration(userId, photoVolumeId).getOrThrow()

            assertEquals(listOf(file3, file2, file1), getIdleFiles())
            assertEquals(listOf(photoVolumeId), photoTagWorkManager.enqueue)
        }

    @Test
    fun `given same client migration and photos when start migration then continue migration`() =
        runTest {
            driveRule.server.run {
                getTagsMigrationStatus(
                    finished = false, anchor = TagsMigrationResponse.Anchor(
                        lastProcessedLinkId = file2.fileId.id,
                        lastProcessedCaptureTime = file2.captureTime.value,
                        lastMigrationTimestamp = 10000,
                        lastClientUid = "test-client-id"
                    )
                )
                getPhotoListings(listOf(file3.toDto(), file2.toDto(), file1.toDto()))
            }

            startTagsMigration(userId, photoVolumeId).getOrThrow()

            assertEquals(listOf(file1), getIdleFiles())
            assertEquals(listOf(photoVolumeId), photoTagWorkManager.enqueue)
        }


    @Test
    fun `given already started migration and photos when start migration then continue migration`() =
        runTest {
            driveRule.server.run {
                getTagsMigrationStatus()
                getPhotoListings(listOf(file3.toDto(), file2.toDto(), file1.toDto()))
            }

            startTagsMigration(userId, photoVolumeId).getOrThrow()
            updateTagsMigrationFileState(photoVolumeId, fileId3, PREPARED).getOrThrow()
            startTagsMigration(userId, photoVolumeId).getOrThrow()

            assertEquals(listOf(file2, file1), getIdleFiles())
            assertEquals(listOf(photoVolumeId, photoVolumeId), photoTagWorkManager.enqueue)
        }

    private fun TagsMigrationFile.toDto() = PhotoListingDto(
        linkId = fileId.id,
        captureTime = captureTime.value,
        hash = null,
        contentHash = null,
    )

    private suspend fun getIdleFiles() = getTagsMigrationFiles(
        userId = userId,
        volumeId = photoVolumeId,
        state = TagsMigrationFile.State.IDLE,
        count = 100,
    ).getOrThrow()
}
