package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.standardShareWithMe
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.userInvitation
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.getUserInvitations
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject


@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetUserInvitationIdCountFlowTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getUserInvitationCountFlow: GetUserInvitationCountFlow

    private val standardShareId = standardShareId()
    private val invitationId = "user-invitation-id-member@proton.me"

    @Test
    fun empty() = runTest {
        driveRule.db.myFiles { }
        driveRule.server.run {
            getUserInvitations()
        }
        val count = getUserInvitationCountFlow(userId).toResult().getOrThrow()

        assertEquals(0, count)
    }

    @Test
    fun `happy path`() = runTest {
        driveRule.db.standardShareWithMe(standardShareId.id) {
            userInvitation(invitationId)
        }
        val count = getUserInvitationCountFlow(userId).toResult().getOrThrow()

        assertEquals(1, count)
    }
}
