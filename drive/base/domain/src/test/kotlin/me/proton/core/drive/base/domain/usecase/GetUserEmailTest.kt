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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetUserEmailTest {
    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var getUserEmail: GetUserEmail

    @Before
    fun setUp() = runTest {
        driveRule.db.user(NullableUserEntity(email = "first@proton.test")) {
            withKey()
            addPrimaryAddress("second@proton.test")
        }
    }

    @Test
    fun `user id`() = runTest {
        assertEquals(
            "second@proton.test",
            getUserEmail(userId),
        )
    }

    @Test
    fun `user and address id`() = runTest {
        assertEquals(
            "first@proton.test",
            getUserEmail(userId, AddressId("address-id-first@proton.test")),
        )
    }
}
