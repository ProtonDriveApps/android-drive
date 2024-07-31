package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.contactEmail
import me.proton.core.drive.db.test.externalInvitation
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.db.test.user
import me.proton.core.drive.db.test.volume
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.test.DriveRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetExternalInvitationFlowTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getExternalInvitationFlow: GetExternalInvitationFlow

    private val standardShareId = standardShareId()


    @Test
    fun one() = runTest {
        driveRule.db.user {
            contactEmail("invitee", isProton = false)
            volume {
                standardShare(standardShareId.id) {
                    externalInvitation("invitee@external.com")
                }
            }
        }

        val invitation = getExternalInvitationFlow(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@external.com",
        ).first()

        assertEquals(
            ShareUser.ExternalInvitee(
                id = "invitation-id-invitee@external.com",
                inviter = "inviter@proton.me",
                email = "invitee@external.com",
                createTime = TimestampS(0),
                permissions = Permissions(0),
                signature = "invitation-signature",
                state = ShareUser.ExternalInvitee.State.PENDING,
                displayName = "invitee"
            ),
            invitation,
        )
    }
}
