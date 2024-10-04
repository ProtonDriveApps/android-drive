package me.proton.core.drive.share.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.NullableUserEntity
import me.proton.core.drive.db.test.addPrimaryAddress
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.standardShareWithMe
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.volume
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
class GetSignatureAddressTest {
    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var getSignatureAddress: GetSignatureAddress

    private val standardShareId = standardShareId()

    @Before
    fun setUp() = runTest {
        driveRule.db.user(NullableUserEntity(email = "first@proton.test")) {
            volume {
                mainShare { }
                standardShareWithMe(standardShareId.id, inviteeEmail = "second@proton.test") {}
            }
            addPrimaryAddress("second@proton.test")
            addPrimaryAddress("third@proton.test")
        }
    }

    @Test
    fun `Given main share id When get address id Then should returns context share address id`() =
        runTest {
            assertEquals(
                "first@proton.test",
                getSignatureAddress(mainShareId).getOrThrow(),
            )
        }

    @Test
    fun `Given standard share id When get address id Then should returns share membership address id`() =
        runTest {
            assertEquals(
                "second@proton.test",
                getSignatureAddress(standardShareId).getOrThrow(),
            )
        }
}
