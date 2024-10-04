package me.proton.core.drive.base.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.NullableUserEntity
import me.proton.core.drive.db.test.addPrimaryAddress
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.withKey
import me.proton.core.drive.test.DriveRule
import me.proton.core.user.domain.entity.AddressId
import org.junit.Assert.assertEquals
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

    @Test
    fun `Given one email When get address id Then returns this address id`() = runTest {
        driveRule.db.user(NullableUserEntity(email = "first@proton.test")) {
            withKey()
        }
        assertEquals(
            AddressId("address-id-first@proton.test"),
            getAddressId(userId),
        )
    }

    @Test
    fun `Given two emails When get address id Then returns the primary email address id`() =
        runTest {
            driveRule.db.user(NullableUserEntity(email = "first@proton.test")) {
                withKey()
                addPrimaryAddress("second@proton.test")
            }
            assertEquals(
                AddressId("address-id-second@proton.test"),
                getAddressId(userId),
            )
        }
}
