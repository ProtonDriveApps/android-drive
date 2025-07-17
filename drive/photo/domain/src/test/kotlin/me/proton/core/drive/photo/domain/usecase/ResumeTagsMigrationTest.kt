package me.proton.core.drive.photo.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.photo
import me.proton.core.drive.db.test.photoVolumeId
import me.proton.core.drive.db.test.userId
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
class ResumeTagsMigrationTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var photoTagWorkManager: TestPhotoTagWorkManager

    @Inject
    lateinit var resumeTagsMigration: ResumeTagsMigration

    @Before
    fun setUp() = runTest {
        driveRule.db.photo { }
    }

    @Test
    fun `happy path`() = runTest {
        resumeTagsMigration(userId, photoVolumeId).getOrThrow()

        assertEquals(listOf(photoVolumeId), photoTagWorkManager.enqueue)
    }
}
