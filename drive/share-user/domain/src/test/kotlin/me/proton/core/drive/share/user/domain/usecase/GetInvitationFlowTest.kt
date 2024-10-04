package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.contactEmail
import me.proton.core.drive.db.test.invitation
import me.proton.core.drive.db.test.standardShareByMe
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
class GetInvitationFlowTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getInvitationFlow: GetInvitationFlow

    private val standardShareId = standardShareId()


    @Test
    fun one() = runTest {
        driveRule.db.user {
            contactEmail("invitee")
            volume {
                standardShareByMe(standardShareId.id) {
                    invitation("invitee@proton.me")
                }
            }
        }

        val invitation = getInvitationFlow(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@proton.me",
        ).first()

        assertEquals(
            ShareUser.Invitee(
                id = "invitation-id-invitee@proton.me",
                inviter = "inviter@proton.me",
                email = "invitee@proton.me",
                createTime = TimestampS(0),
                permissions = Permissions(0),
                keyPacket = "invitation-key-packet",
                keyPacketSignature = "invitation-key-packet-signature",
                displayName = "invitee"
            ),
            invitation,
        )
    }
}
