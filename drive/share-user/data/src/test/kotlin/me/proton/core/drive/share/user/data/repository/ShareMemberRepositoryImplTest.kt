package me.proton.core.drive.share.user.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.member
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.repository.ShareMemberRepository
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.clear
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getMembers
import me.proton.core.drive.test.api.updateMember
import me.proton.core.network.domain.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ShareMemberRepositoryImplTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var repository: ShareMemberRepository

    private val standardShareId = standardShareId()

    @Test
    fun hasMembers_empty() = runTest {
        driveRule.db.run {
            standardShare(standardShareId.id)
        }
        assertFalse(repository.hasMembers(standardShareId))
    }

    @Test
    fun hasMembers_one() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            member("member@proton.me")
        }

        assertTrue(repository.hasMembers(standardShareId))
    }

    @Test
    fun getMembersFlow_empty() = runTest {
        driveRule.db.standardShare(standardShareId.id)
        assertEquals(emptyList<ShareUser>(), repository.getMembersFlow(standardShareId, 500).first())
    }

    @Test
    fun getMembersFlow_one() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            member("member@proton.me")
        }

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
                    sessionKeySignature = "member-session-key-signature",
                )
            ), repository.getMembersFlow(standardShareId, 500).first()
        )
    }

    @Test
    fun fetchMembers() = runTest {
        driveRule.db.standardShare(standardShareId.id)
        driveRule.server.getMembers("member@proton.me")
        val fetchMembers = repository.fetchMembers(standardShareId)
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
                    sessionKeySignature = "member-session-key-signature",
                )
            ), fetchMembers
        )
    }

    @Test
    fun `fetchMembers should delete all previous members`() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.getMembers("member1@proton.me")
        repository.fetchMembers(standardShareId)
        driveRule.server.clear()

        driveRule.server.getMembers("member2@proton.me")
        repository.fetchMembers(standardShareId)

        assertEquals(
            listOf(
                ShareUser.Member(
                    id = "member-id-member2@proton.me",
                    inviter = "inviter@proton.me",
                    email = "member2@proton.me",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    keyPacket = "member-key-packet",
                    keyPacketSignature = "member-key-packet-signature",
                    sessionKeySignature = "member-session-key-signature",
                )
            ), repository.getMembersFlow(standardShareId, 500).first()
        )
    }

    @Test
    fun updateMember() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.updateMember()

        repository.updateMember(
            shareId = standardShareId,
            memberId = "member-id-member@proton.me",
            permissions = Permissions().add(Permissions.Permission.READ),
        )
    }

    @Test(expected = ApiException::class)
    fun updateMember_error() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.updateMember { errorResponse() }

        repository.updateMember(
            shareId = standardShareId,
            memberId = "member-id-member@proton.me",
            permissions = Permissions().add(Permissions.Permission.READ),
        )
    }
}
