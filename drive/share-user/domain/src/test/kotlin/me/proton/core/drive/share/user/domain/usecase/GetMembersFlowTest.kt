package me.proton.core.drive.share.user.domain.usecase

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.base.domain.extension.filterSuccessOrError
import me.proton.core.drive.base.domain.extension.toResult
import me.proton.core.drive.db.test.folder
import me.proton.core.drive.db.test.mainShareId
import me.proton.core.drive.db.test.myFiles
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.link.domain.entity.FolderId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.getInvitations
import me.proton.core.drive.test.api.getMembers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class GetMembersFlowTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var getMembersFlow: GetMembersFlow

    private val standardShareId = standardShareId()
    private val folderId = FolderId(mainShareId, "folder-id")

    @Before
    fun setUp() = runTest {
        driveRule.db.run {
            standardShare(standardShareId.id)
            myFiles {
                folder(
                    id = folderId.id,
                    sharingDetailsShareId = standardShareId.id,
                )
            }
        }
    }

    @Test
    fun empty() = runTest {
        driveRule.server.getMembers()

        val members =
            getMembersFlow(standardShareId).filterSuccessOrError().toResult().getOrThrow()

        assertEquals(emptyList<ShareUser>(), members)
    }

    @Test
    fun one() = runTest {
        driveRule.server.getMembers("member@proton.me")

        val members =
            getMembersFlow(standardShareId).filterSuccessOrError().toResult().getOrThrow()

        assertEquals(
            listOf(
                ShareUser.Member(
                    id = "member-id-member@proton.me",
                    inviter = "inviter@proton.me",
                    email = "member@proton.me",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    keyPacket = "member-key-packet",
                    keyPacketSignature = "member-key-packet-signature",
                    sessionKeySignature="member-session-key-signature",
                )
            ), members
        )
    }
}
