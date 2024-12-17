package me.proton.core.drive.entitlement.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.db.test.entitlements
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.entitlement.domain.entity.Entitlement
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.entitlements
import me.proton.core.drive.test.api.response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class EntitlementRepositoryImplTest {

    @get:Rule
    var driveRule = DriveRule(this)

    @Inject
    lateinit var repository: EntitlementRepositoryImpl

    @Test
    fun empty() = runTest {
        val entitlement =
            repository.getEntitlement(userId, Entitlement.Key.PublicCollaboration).first()

        assertNull(entitlement)
    }

    @Test
    fun getBoolean() = runTest {
        driveRule.db.user {
            entitlements(mapOf(Entitlement.Key.PublicCollaboration.name to true))
        }

        val entitlement =
            repository.getEntitlement(userId, Entitlement.Key.PublicCollaboration).first()

        assertEquals(Entitlement.PublicCollaboration(true), entitlement)
    }

    @Test
    fun getLong() = runTest {
        driveRule.db.user {
            entitlements(mapOf(Entitlement.Key.MaxRevisionCount.name to 3))
        }

        val entitlement =
            repository.getEntitlement(userId, Entitlement.Key.MaxRevisionCount).first()

        assertEquals(Entitlement.MaxRevisionCount(3), entitlement)
    }

    @Test
    fun fetchAndStoreEntitlements() = runTest {
        driveRule.db.user {}
        driveRule.server.entitlements {
            response {
                // language=json
                """
                    {
                        "Code": 1000,
                        "Entitlements": {
                            "PublicCollaboration": true
                        }
                    }
                """.trimIndent()
            }
        }

        repository.fetchAndStoreEntitlements(userId)

        assertEquals(
            Entitlement.PublicCollaboration(true),
            repository.getEntitlement(userId, Entitlement.Key.PublicCollaboration).first(),
        )
    }
}
