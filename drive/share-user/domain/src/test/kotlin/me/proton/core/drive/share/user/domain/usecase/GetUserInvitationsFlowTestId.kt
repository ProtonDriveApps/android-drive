package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.standardShareWithMe
import me.proton.core.drive.db.test.userId
import me.proton.core.drive.db.test.userInvitation
import me.proton.core.drive.db.test.volumeId
import me.proton.core.drive.share.user.domain.entity.UserInvitationId
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
class GetUserInvitationsFlowTestId {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getUserInvitationsFlow: GetUserInvitationsFlow

    private val standardShareId = standardShareId()
    private val invitationId = "user-invitation-id-member@proton.me"
    private val id = UserInvitationId(volumeId, standardShareId, invitationId)

    @Test
    fun empty() = runTest {
        driveRule.db.myFiles { }
        driveRule.server.run {
            getUserInvitations()
        }
        val invitations = getUserInvitationsFlow(userId).toResult().getOrThrow()

        assertEquals(emptyList<UserInvitationId>(), invitations)
    }

    @Test
    fun `happy path`() = runTest {
        driveRule.db.standardShareWithMe(standardShareId.id) {
            userInvitation(invitationId)
        }
        val invitations = getUserInvitationsFlow(userId).toResult().getOrThrow()

        assertEquals(id, invitations.first().id)
    }
}
