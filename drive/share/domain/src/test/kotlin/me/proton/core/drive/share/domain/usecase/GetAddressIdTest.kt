package me.proton.core.drive.share.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.NullableUserEntity
import me.proton.core.drive.db.test.addPrimaryAddress
import me.proton.core.drive.db.test.mainShare
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.standardShareWithMe
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.test.DriveRule
import me.proton.core.user.domain.entity.AddressId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetAddressIdTest {
    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var getAddressId: GetAddressId

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
    fun `Given volume id When get address id Then should returns context share address id`() =
        runTest {
            assertEquals(
                AddressId("address-id-first@proton.test"),
                getAddressId(userId, volumeId).getOrThrow(),
            )
        }

    @Test
    fun `Given main share id When get address id Then should returns context share address id`() =
        runTest {
            assertEquals(
                AddressId("address-id-first@proton.test"),
                getAddressId(mainShareId).getOrThrow(),
            )
        }

    @Test
    fun `Given standard share id When get address id Then should returns share membership address id`() =
        runTest {
            assertEquals(
                AddressId("address-id-second@proton.test"),
                getAddressId(standardShareId).getOrThrow(),
            )
        }
}
