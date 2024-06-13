package me.proton.core.drive.share.user.data.repository

import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.proton.core.drive.base.domain.entity.Permissions
import me.proton.core.drive.base.domain.entity.Permissions.Permission.READ
import me.proton.core.drive.base.domain.entity.TimestampS
import me.proton.core.drive.db.test.invitation
import me.proton.core.drive.db.test.standardShare
import me.proton.core.drive.db.test.standardShareId
import me.proton.core.drive.share.crypto.domain.entity.ShareInvitationRequest
import me.proton.core.drive.share.user.domain.entity.ShareUser
import me.proton.core.drive.share.user.domain.repository.ShareInvitationRepository
import me.proton.core.drive.test.DriveRule
import me.proton.core.drive.test.api.clear
import me.proton.core.drive.test.api.createInvitation
import me.proton.core.drive.test.api.deleteInvitation
import me.proton.core.drive.test.api.errorResponse
import me.proton.core.drive.test.api.getInvitations
import me.proton.core.drive.test.api.sendEmail
import me.proton.core.drive.test.api.updateInvitation
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
class ShareInvitationRepositoryImplTest {

    @get:Rule
    val driveRule = DriveRule(this)

    @Inject
    lateinit var repository: ShareInvitationRepository

    private val standardShareId = standardShareId()

    @Test
    fun hasInvitations_empty() = runTest {
        driveRule.db.run {
            standardShare(standardShareId.id)
        }
        assertFalse(repository.hasInvitations(standardShareId))
    }

    @Test
    fun hasInvitations_one() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            invitation("invitee@proton.me")
        }

        assertTrue(repository.hasInvitations(standardShareId))
    }

    @Test
    fun getInvitationsFlow_empty() = runTest {
        driveRule.db.standardShare(standardShareId.id)
        assertEquals(
            emptyList<ShareUser>(),
            repository.getInvitationsFlow(standardShareId, 500).first()
        )
    }

    @Test
    fun getInvitationsFlow_one() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            invitation("invitee@proton.me")
        }

        assertEquals(
            listOf(
                ShareUser.Invitee(
                    id = "invitation-id-invitee@proton.me",
                    inviter = "inviter@proton.me",
                    email = "invitee@proton.me",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    keyPacket = "invitation-key-packet",
                    keyPacketSignature = "invitation-key-packet-signature",
                )
            ), repository.getInvitationsFlow(standardShareId, 500).first()
        )
    }

    @Test
    fun getInvitationFlow() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            invitation("invitee@proton.me")
        }

        assertEquals(
            ShareUser.Invitee(
                id = "invitation-id-invitee@proton.me",
                inviter = "inviter@proton.me",
                email = "invitee@proton.me",
                createTime = TimestampS(0),
                permissions = Permissions(0),
                keyPacket = "invitation-key-packet",
                keyPacketSignature = "invitation-key-packet-signature",
            ),
            repository.getInvitationFlow(
                shareId = standardShareId,
                invitationId = "invitation-id-invitee@proton.me",
            ).first(),
        )
    }

    @Test
    fun fetchInvitations() = runTest {
        driveRule.db.standardShare(standardShareId.id)
        driveRule.server.getInvitations("invitee@proton.me")
        val fetchInvitations = repository.fetchInvitations(standardShareId)
        assertEquals(
            listOf(
                ShareUser.Invitee(
                    id = "invitation-id-invitee@proton.me",
                    inviter = "inviter@proton.me",
                    email = "invitee@proton.me",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    keyPacket = "invitation-key-packet",
                    keyPacketSignature = "invitation-key-packet-signature",
                )
            ), fetchInvitations
        )
    }

    @Test
    fun `fetchInvitations should delete all previous invitations`() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.getInvitations("invitee1@proton.me")
        repository.fetchInvitations(standardShareId)
        driveRule.server.clear()

        driveRule.server.getInvitations("invitee2@proton.me")
        repository.fetchInvitations(standardShareId)

        assertEquals(
            listOf(
                ShareUser.Invitee(
                    id = "invitation-id-invitee2@proton.me",
                    inviter = "inviter@proton.me",
                    email = "invitee2@proton.me",
                    createTime = TimestampS(0),
                    permissions = Permissions(0),
                    keyPacket = "invitation-key-packet",
                    keyPacketSignature = "invitation-key-packet-signature",
                )
            ), repository.getInvitationsFlow(standardShareId, 500).first()
        )
    }

    @Test
    fun createInvitations() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.createInvitation()
        val shareUserInvitee = repository.createInvitation(
            standardShareId, ShareInvitationRequest(
                inviterEmail = "inviter@proton.me",
                inviteeEmail = "invitee@proton.me",
                permissions = Permissions(0L),
                keyPacket = "invitation-key-packet",
                keyPacketSignature = "invitation-key-packet-signature",
            )
        )
        assertEquals(
            ShareUser.Invitee(
                id = "invitation-id-invitee@proton.me",
                inviter = "inviter@proton.me",
                email = "invitee@proton.me",
                createTime = TimestampS(0),
                permissions = Permissions(0),
                keyPacket = "invitation-key-packet",
                keyPacketSignature = "invitation-key-packet-signature",
            ), shareUserInvitee
        )
    }

    @Test(expected = ApiException::class)
    fun createInvitations_error() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.createInvitation { errorResponse() }

        repository.createInvitation(
            standardShareId, ShareInvitationRequest(
                inviterEmail = "inviter@proton.me",
                inviteeEmail = "invitee@proton.me",
                permissions = Permissions(0L),
                keyPacket = "key-packet",
                keyPacketSignature = "key-packet-signature",
            )
        )
    }

    @Test
    fun updateInvitation() = runTest {
        driveRule.db.standardShare(standardShareId.id) {
            invitation("invitee@proton.me")
        }

        driveRule.server.updateInvitation()

        repository.updateInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@proton.me",
            permissions = Permissions.viewer,
        )
    }

    @Test(expected = ApiException::class)
    fun updateInvitation_error() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.updateInvitation { errorResponse() }

        repository.updateInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@proton.me",
            permissions = Permissions.viewer,
        )
    }

    @Test
    fun deleteInvitation() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.deleteInvitation()

        repository.deleteInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@proton.me",
        )
    }

    @Test(expected = ApiException::class)
    fun deleteInvitation_error() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.deleteInvitation { errorResponse() }

        repository.deleteInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@proton.me",
        )
    }

    @Test
    fun resendInvitation() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.sendEmail()

        repository.resendInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@proton.me",
        )
    }

    @Test(expected = ApiException::class)
    fun resendInvitation_error() = runTest {
        driveRule.db.standardShare(standardShareId.id)

        driveRule.server.sendEmail { errorResponse() }

        repository.resendInvitation(
            shareId = standardShareId,
            invitationId = "invitation-id-invitee@proton.me",
        )
    }
}
