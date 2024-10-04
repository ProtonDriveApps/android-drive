package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.Permissions.Permission.READ
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.db.test.invitation
import me.proton.core.drive.db.test.standardShareByMe
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.updateInvitation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject


@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class UpdateInvitationPermissionsTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var updateInvitationPermissions: UpdateInvitationPermissions

    @Inject
    lateinit var getInvitationFlow: GetInvitationFlow

    private val standardShareId = standardShareId()
    private val invitationId = "invitation-id-invitee@proton.me"

    @Before
    fun setUp() = runTest {
        driveRule.db.standardShareByMe(standardShareId.id) {
            invitation("invitee@proton.me")
        }
    }

    @Test
    fun `happy path`() = runTest {
        driveRule.server.run {
            updateInvitation()
        }

        val result = updateInvitationPermissions(
            shareId = standardShareId,
            invitationId = invitationId,
            permissions = Permissions().add(READ),
        ).filterSuccessOrError().last()

        assertTrue("${result.javaClass} should be Success", result is DataResult.Success)
        assertEquals(
            Permissions().add(READ),
            getInvitationFlow(standardShareId, invitationId).first().permissions,
        )
    }

    @Test
    fun fails() = runTest {
        driveRule.server.run {
            updateInvitation { errorResponse()}
        }

        val result = updateInvitationPermissions(
            shareId = standardShareId,
            invitationId = invitationId,
            permissions = Permissions().add(READ),
        ).filterSuccessOrError().last()

        assertTrue("${result.javaClass} should be Error", result is DataResult.Error)
        assertEquals(
            Permissions(),
            getInvitationFlow(standardShareId, invitationId).first().permissions,
        )
    }
}
