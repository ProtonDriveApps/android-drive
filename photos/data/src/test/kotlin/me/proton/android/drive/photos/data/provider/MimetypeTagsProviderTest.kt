package me.proton.android.drive.photos.data.provider

import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.link.domain.entity.PhotoTag
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MimetypeTagsProviderTest {
    @get:Rule
    val driveRule = DriveRule(this)

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Inject
    lateinit var provider: MimetypeTagsProvider

    @Before fun setUp() {
        shadowOf(MimeTypeMap.getSingleton()).apply {
            addExtensionMimeTypeMapping("dng", "image/x-adobe-dng")
            addExtensionMimeTypeMapping("jpg", "image/jpeg")
            addExtensionMimeTypeMapping("mp4", "video/mp4")
        }
    }

    @Test
    fun noTags() = runTest {
        val file = temporaryFolder.newFile("photo.jpg")

        val tags = provider(file.toUri().toString())

        assertEquals(emptyList<PhotoTag>(), tags)
    }

    @Test
    fun rawTags() = runTest {
        val file = temporaryFolder.newFile("photo.dng")

        val tags = provider(file.toUri().toString())

        assertEquals(listOf(PhotoTag.Raw), tags)
    }

    @Test
    fun videoTags() = runTest {
        val file = temporaryFolder.newFile("video.mp4")

        val tags = provider(file.toUri().toString())

        assertEquals(listOf(PhotoTag.Videos), tags)
    }
}
