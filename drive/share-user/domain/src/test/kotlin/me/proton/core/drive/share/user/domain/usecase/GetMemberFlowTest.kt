package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.contactEmail
import me.proton.core.drive.db.test.member
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
class GetMemberFlowTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getMemberFlow: GetMemberFlow

    private val standardShareId = standardShareId()

    @Test
    fun test() = runTest {
        driveRule.db.user {
            contactEmail("member")
            volume {
                standardShareByMe(standardShareId.id) {
                    member("member@proton.me")
                }
            }
        }

        val member = getMemberFlow(
            shareId = standardShareId,
            memberId = "member-id-member@proton.me",
        ).first()

        assertEquals(
            ShareUser.Member(
                id = "member-id-member@proton.me",
                inviter = "inviter@proton.me",
                email = "member@proton.me",
                createTime = TimestampS(0),
                permissions = Permissions(0),
                keyPacket = "member-key-packet",
                keyPacketSignature = "member-key-packet-signature",
                sessionKeySignature = "member-session-key-signature",
                displayName = "member",
            ),
            member,
        )
    }
}
