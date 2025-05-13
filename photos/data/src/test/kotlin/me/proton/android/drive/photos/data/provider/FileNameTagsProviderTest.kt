package me.proton.android.drive.photos.data.provider

import androidx.core.net.toUri
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class FileNameTagsProviderTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Inject
    lateinit var provider: FileNameTagsProvider

    @Test
    fun noTags() = runTest {
        val file = temporaryFolder.newFile("photo.jpg")

        val tags = provider(file.toUri().toString())

        assertEquals(emptyList<PhotoTag>(), tags)
    }

    @Test
    fun screenshotTags() = runTest {
        val file = temporaryFolder.newFile("Screenshot_1.jpg")

        val tags = provider(file.toUri().toString())

        assertEquals(listOf(PhotoTag.Screenshots), tags)
    }
}
