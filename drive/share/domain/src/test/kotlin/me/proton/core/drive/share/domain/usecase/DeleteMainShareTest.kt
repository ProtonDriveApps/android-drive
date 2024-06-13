package me.proton.core.drive.share.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.deleteShare
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DeleteMainShareTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var deleteShare: DeleteShare

    private val shareId = mainShareId

    @Before
    fun setUp() = runTest {
        driveRule.server.deleteShare()
        driveRule.db.user {
            volume {
                mainShare {}
            }
        }
    }

    @Test
    fun `no locallyOnly and no force`() = runTest {
        deleteShare(shareId).getOrThrow()
    }

    @Test
    fun `locallyOnly and no force`() = runTest {
        deleteShare(shareId, locallyOnly = true, force = false).getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun `no locallyOnly and force`() = runTest {
        deleteShare(shareId, locallyOnly = false, force = true).getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun `locallyOnly and force`() = runTest {
        deleteShare(shareId, locallyOnly = true, force = true).getOrThrow()
    }
}

