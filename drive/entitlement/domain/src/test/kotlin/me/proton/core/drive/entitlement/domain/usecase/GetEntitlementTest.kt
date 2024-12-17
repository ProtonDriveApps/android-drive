package me.proton.core.drive.entitlement.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.entitlements
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.entitlement.domain.entity.Entitlement
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetEntitlementTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var getEntitlement: GetEntitlement

    @Test
    fun empty() = runTest {
        val entitlement =
            getEntitlement(userId, Entitlement.Key.PublicCollaboration).toResult().getOrThrow()

        assertNull(entitlement)
    }

    @Test
    fun get() = runTest {
        driveRule.db.user {
            entitlements(mapOf(Entitlement.Key.PublicCollaboration.name to true))
        }
        val entitlement =
            getEntitlement(userId, Entitlement.Key.PublicCollaboration).toResult().getOrThrow()

        assertEquals(Entitlement.PublicCollaboration(true), entitlement)
    }

}
